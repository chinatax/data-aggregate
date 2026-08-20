package com.dataagg.start.scheduler;

import com.dataagg.common.entity.NewsArticle;
import com.dataagg.service.service.NewsArticleService;
import com.dataagg.start.service.SchedulerTaskTracker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class NewsFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsFetchScheduler.class);

    private static final String[] CATEGORIES = {
        "top", "world", "politics", "science", "technology",
        "health", "entertainment", "environment", "food",
        "business"
    };

    private static final String[] TAX_QUERIES = {
        "税", "税收", "税务", "纳税", "征税", "税率", "关税"
    };

    private static final String REDIS_KEY = "news:latest";
    private static final int REDIS_TTL_MINUTES = 15;
    private static final int MAX_REDIS_ITEMS = 200;
    private static final int MAX_DB_DAYS = 3;
    private static final int MAX_CONCURRENT = 3;

    private final ExecutorService executor = Executors.newFixedThreadPool(CATEGORIES.length + TAX_QUERIES.length);
    private final Semaphore rateLimiter = new Semaphore(MAX_CONCURRENT);
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SchedulerTaskTracker tracker;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NewsArticleService newsArticleService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${newsdata.api-key}")
    private String apiKey;

    @Value("${newsdata.tax-api-key}")
    private String taxApiKey;

    @PostConstruct
    public void registerTasks() {
        tracker.register("fetchNews", "新闻采集", "从NewsData.io采集10分类+7税务关键词新闻，入库并写Redis缓存", "新闻数据",
                "FIXED_DELAY", "10min", this::doFetchNews);
    }

    @Scheduled(initialDelayString = "${news.fetch.initialDelay:30000}", fixedDelayString = "${news.fetch.interval:600000}")
    public void fetchNews() {
        tracker.markRunning("fetchNews");
        try {
            doFetchNews();
            tracker.markSuccess("fetchNews");
        } catch (Exception e) {
            tracker.markFailed("fetchNews", e.getMessage());
            log.error("fetchNews failed", e);
        }
    }

    private void doFetchNews() {
        log.info("News fetch started, categories={}", CATEGORIES.length);
        long start = System.currentTimeMillis();

        List<CompletableFuture<List<NewsArticle>>> futures = new ArrayList<>();
        for (String q : TAX_QUERIES) {
            futures.add(CompletableFuture.supplyAsync(() -> fetchQuery(q, "tax"), executor));
        }
        for (String cat : CATEGORIES) {
            futures.add(CompletableFuture.supplyAsync(() -> fetchCategory(cat), executor));
        }

        int fetched = 0;
        int saved = 0;
        Set<String> seenLinks = new LinkedHashSet<>();

        for (CompletableFuture<List<NewsArticle>> future : futures) {
            try {
                List<NewsArticle> items = future.get();
                fetched += items.size();
                for (NewsArticle item : items) {
                    if (seenLinks.add(item.getLink()) && !newsArticleService.existsByLink(item.getLink())) {
                        newsArticleService.save(item);
                        saved++;
                    }
                }
            } catch (Exception e) {
                log.warn("Category fetch failed: {}", e.getMessage());
            }
        }

        newsArticleService.deleteOlderThan(LocalDateTime.now().minusDays(MAX_DB_DAYS));

        try {
            List<NewsArticle> cached = newsArticleService.listLatest(MAX_REDIS_ITEMS);
            String json = objectMapper.writeValueAsString(cached);
            redisTemplate.opsForValue().set(REDIS_KEY, json, Duration.ofMinutes(REDIS_TTL_MINUTES));
            log.info("Redis updated: {} items, TTL={}min", cached.size(), REDIS_TTL_MINUTES);
        } catch (Exception e) {
            log.error("Redis write failed", e);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("News fetch done: fetched={}, saved={}, elapsed={}ms", fetched, saved, elapsed);
    }

    private List<NewsArticle> fetchCategory(String category) {
        List<NewsArticle> list = new ArrayList<>();
        try {
            acquireSlot();
            String url = UriComponentsBuilder.fromHttpUrl("https://newsdata.io/api/1/latest")
                .queryParam("apikey", apiKey)
                .queryParam("category", category)
                .queryParam("language", "zh")
                .queryParam("size", 10)
                .build()
                .toUriString();
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);

            if (!"success".equals(root.path("status").asText())) {
                return list;
            }

            for (JsonNode item : root.path("results")) {
                NewsArticle a = new NewsArticle();
                a.setTitle(item.path("title").asText());
                a.setSource(item.path("source_id").asText());
                a.setDescription(truncate(item.path("description").asText(), 800));
                a.setLink(item.path("link").asText());
                a.setImageUrl(item.path("image_url").asText(null));
                a.setCountry(item.path("country").asText(""));
                a.setCategory(category);
                String pubStr = item.path("pubDate").asText();
                try {
                    a.setPubDate(LocalDateTime.parse(pubStr, fmt));
                } catch (Exception e) {
                    a.setPubDate(LocalDateTime.now());
                }
                a.setCreateTime(LocalDateTime.now());
                list.add(a);
            }
        } catch (Exception e) {
            log.warn("Fetch category {} failed: {}", category, e.getMessage());
        } finally {
            rateLimiter.release();
        }
        return list;
    }

    private List<NewsArticle> fetchQuery(String query, String tag) {
        List<NewsArticle> list = new ArrayList<>();
        try {
            acquireSlot();
            String url = UriComponentsBuilder.fromHttpUrl("https://newsdata.io/api/1/latest")
                .queryParam("apikey", taxApiKey)
                .queryParam("q", query)
                .queryParam("language", "zh")
                .queryParam("size", 10)
                .build()
                .toUriString();
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);

            int total = root.path("totalResults").asInt();
            for (JsonNode item : root.path("results")) {
                NewsArticle a = new NewsArticle();
                a.setTitle(item.path("title").asText());
                a.setSource(item.path("source_id").asText());
                a.setDescription(truncate(item.path("description").asText(), 800));
                a.setLink(item.path("link").asText());
                a.setImageUrl(item.path("image_url").asText(null));
                a.setCountry(item.path("country").asText(""));
                a.setCategory(tag);
                String pubStr = item.path("pubDate").asText();
                try {
                    a.setPubDate(LocalDateTime.parse(pubStr, fmt));
                } catch (Exception e) {
                    a.setPubDate(LocalDateTime.now());
                }
                a.setCreateTime(LocalDateTime.now());
                list.add(a);
            }
            log.info("Tax query '{}': total={}, got={}", query, total, list.size());
        } catch (Exception e) {
            log.warn("Fetch query {} failed: {}", query, e.getMessage());
        } finally {
            rateLimiter.release();
        }
        return list;
    }

    private void acquireSlot() {
        try {
            if (!rateLimiter.tryAcquire(10, TimeUnit.SECONDS)) {
                log.warn("Timed out waiting for rate-limit slot");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen);
    }
}
