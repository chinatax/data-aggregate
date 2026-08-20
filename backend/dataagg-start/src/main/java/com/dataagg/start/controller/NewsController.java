package com.dataagg.start.controller;

import com.dataagg.common.entity.NewsArticle;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.NewsArticleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private static final String REDIS_KEY = "news:latest";
    private static final int DEFAULT_LIMIT = 200;

    private static final String TAX_CATEGORY = "tax";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NewsArticleService newsArticleService;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @GetMapping
    public Result<?> getNews(
            @RequestParam(defaultValue = "200") int limit) {
        try {
            // 1. 尝试从 Redis 读取
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null && !cached.isEmpty()) {
                List<NewsArticle> list = mapper.readValue(cached,
                    new TypeReference<List<NewsArticle>>() {});
                // 排除税收类（税收快讯已独立展示）
                list = list.stream()
                    .filter(a -> !"tax".equals(a.getCategory()))
                    .collect(Collectors.toList());
                if (limit < list.size()) {
                    list = list.subList(0, limit);
                }
                return Result.success(toSimpleList(list));
            }

            // 2. Redis 未命中，从 MySQL 读取（排除税收类）
            List<NewsArticle> dbList = newsArticleService.listLatestExcludingTax(limit);
            return Result.success(toSimpleList(dbList));
        } catch (Exception e) {
            return Result.error("获取新闻失败: " + e.getMessage());
        }
    }

    @GetMapping("/tax")
    public Result<?> getTaxNews(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<NewsArticle> list = newsArticleService.listByCategory(TAX_CATEGORY, limit);
            return Result.success(toSimpleList(list));
        } catch (Exception e) {
            return Result.error("获取税收新闻失败: " + e.getMessage());
        }
    }

    private List<Object> toSimpleList(List<NewsArticle> articles) {
        return articles.stream().map(a -> new Object() {
            public final String title = a.getTitle();
            public final String source = a.getSource();
            public final String description = a.getDescription();
            public final String link = a.getLink();
            public final String pubDate = a.getPubDate() != null ? a.getPubDate().toString() : "";
            public final String imageUrl = a.getImageUrl();
            public final String country = a.getCountry();
            public final String category = a.getCategory();
        }).map(o -> (Object) o).toList();
    }
}
