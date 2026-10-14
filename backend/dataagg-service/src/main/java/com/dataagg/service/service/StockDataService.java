package com.dataagg.service.service;

import com.dataagg.common.dto.*;
import com.dataagg.common.dto.StockKlineVO;
import com.dataagg.common.dto.StockQualityScoreVO;
import com.dataagg.common.entity.StockDragonBoard;
import com.dataagg.common.entity.StockFinancial;
import com.dataagg.common.entity.StockGlobalNews;
import com.dataagg.common.entity.StockHotTopic;
import com.dataagg.common.entity.StockIndustryRank;
import com.dataagg.common.entity.StockLockupExpiry;
import com.dataagg.common.entity.StockNewsFlash;
import com.dataagg.common.entity.StockQualityScore;
import com.dataagg.common.entity.StockBoardCount;
import com.dataagg.common.entity.StockBoardDaily;
import com.dataagg.common.entity.StockDailyAmount;
import com.dataagg.common.entity.StockDailyDisplay;
import com.dataagg.common.entity.StockHolderNum;
import com.dataagg.common.entity.StockHolderTop10;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StockDataService {

    private static final Logger log = LoggerFactory.getLogger(StockDataService.class);

    private static final String REDIS_INDICES = "stock:indices";
    private static final String REDIS_NEWSFLASH = "stock:newsflash";
    private static final String REDIS_HOTTOPICS = "stock:hottopics";
    private static final String REDIS_INDUSTRY = "stock:industry";
    private static final String REDIS_NORTHBOUND = "stock:northbound";
    private static final String REDIS_MARGIN = "stock:margin";
    private static final String REDIS_DRAGONBOARD = "stock:dragonboard";
    private static final String REDIS_LOCKUP = "stock:lockup";
    private static final String REDIS_GLOBALNEWS = "stock:globalnews";
    private static final String REDIS_BOARDLADDER = "stock:boardladder";
    private static final String REDIS_HOLDERNUM = "stock:holdernum";
    private static final String REDIS_HOLDERTOP10 = "stock:holdertop10";
    private static final String REDIS_QUALITY = "stock:quality";
    private static final String REDIS_KLINE_PREFIX = "stock:kline:";
    private static final String REDIS_DAILY_AMOUNT = "stock:daily:amount";
    private static final String REDIS_DAILY_DISPLAY = "stock:daily:display";
    private static final String REDIS_BOARD_COUNT = "stock:daily:board-count";

    // L1 本地缓存（Caffeine）
    private final Cache<String, List<StockNewsFlashVO>> newsFlashL1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofSeconds(20)).build();
    private final Cache<String, List<StockHotTopicVO>> hotTopicsL1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofSeconds(60)).build();
    private final Cache<String, List<StockDragonBoardVO>> dragonBoardL1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofSeconds(30)).build();
    private final Cache<String, List<StockHolderNumVO>> holderNumL1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofSeconds(60)).build();
    private final Cache<String, List<StockHolderTop10VO>> holderTop10L1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofMinutes(5)).build();
    private final Cache<String, List<StockQualityScoreVO>> qualityL1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofMinutes(5)).build();
    private final Cache<String, List<StockIndexVO>> indicesL1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofSeconds(10)).build();
    private final Cache<String, List<StockKlineVO>> klineL1 =
            Caffeine.newBuilder().maximumSize(500).expireAfterWrite(Duration.ofMinutes(5)).build();
    private final Cache<String, List<StockDailyAmountVO>> dailyAmountL1 =
            Caffeine.newBuilder().maximumSize(200).expireAfterWrite(Duration.ofMinutes(30)).build();
    private final Cache<String, List<StockDailyAmountVO>> dailyDisplayL1 =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofMinutes(10)).build();
    private volatile List<Map<String, String>> stockNamesCache;
    private volatile long stockNamesCacheTs;

    /** 股票分类缓存：code -> [name, industry, concept, region] */
    private final Map<String, String[]> stockClassCache = new ConcurrentHashMap<>();

    // 20 个指数：主板 + 深市 + 板块 + 行业主题
    private static final String INDEX_CODES =
        "sh000001,sh000016,sh000300,sh000905,sh000852,sh000688,sh000698,sh000010,"
        + "sz399001,sz399006,sz399005,sz399330,sz399293,sz399959,"
        + "sh000922,sz980017,sh000941,sz399975,sh000933,sh000997";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @jakarta.annotation.PostConstruct
    private void initObjectMapper() {
        objectMapper.enable(
            com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature());
    }

    @Autowired
    private StockNewsFlashService newsFlashService;

    @Autowired
    private StockHotTopicService hotTopicService;

    @Autowired
    private StockIndustryRankService industryRankService;

    @Autowired
    private StockDragonBoardService dragonBoardService;

    @Autowired
    private StockLockupExpiryService lockupExpiryService;

    @Autowired
    private StockGlobalNewsService globalNewsService;

    @Autowired
    private StockBoardService boardService;

    @Autowired
    private StockHolderNumService holderNumService;

    @Autowired
    private StockHolderTop10Service holderTop10Service;

    @Autowired
    private StockFinancialService stockFinancialService;
    @Autowired
    private StockQualityScoreService stockQualityScoreService;

    @Autowired
    private StockDailyAmountService dailyAmountService;

    @Autowired
    private StockDailyDisplayService dailyDisplayService;

    // ==================== 1. 指数行情 ====================

    public List<StockIndexVO> fetchAndCacheIndices() {
        try {
            String url = "https://qt.gtimg.cn/q=" + INDEX_CODES;
            String raw = restTemplate.getForObject(url, String.class);
            if (raw == null || raw.isEmpty()) return Collections.emptyList();

            List<StockIndexVO> indices = new ArrayList<>();
            String[] lines = raw.split(";");
            for (String line : lines) {
                if (line.isEmpty() || !line.contains("=")) continue;
                String val = line.split("=")[1];
                if (val == null || val.isEmpty()) continue;
                val = val.replaceAll("^\"|\"$", "");
                String[] f = val.split("~");
                if (f.length < 33) continue;
                StockIndexVO vo = StockIndexVO.builder()
                        .name(f[1])
                        .code(line.split("=\"")[0].replace("v_", "").trim())
                        .price(new BigDecimal(f[3]))
                        .change(new BigDecimal(f[31]))
                        .changePct(new BigDecimal(f[32]))
                        .build();
                indices.add(vo);
            }

            String json = objectMapper.writeValueAsString(indices);
            redisTemplate.opsForValue().set(REDIS_INDICES, json, Duration.ofSeconds(60));
            return indices;
        } catch (Exception e) {
            log.warn("Fetch indices failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 2. 财联社快讯 ====================

    public List<StockNewsFlashVO> fetchAndCacheNewsFlashes() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Referer", "https://www.cls.cn/");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = "https://www.cls.cn/api/cache?rn=50&name=telegraph";
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode rollData = root.path("data").path("roll_data");
            if (!rollData.isArray()) return Collections.emptyList();

            int saved = 0;
            for (JsonNode item : rollData) {
                String newsId = item.path("id").asText();
                if (newsId.isEmpty() || newsFlashService.existsByNewsId(newsId)) continue;

                String title = item.path("title").asText();
                String content = item.path("content").asText();
                if (title.isEmpty()) title = item.path("brief").asText();
                if (content.isEmpty()) content = item.path("brief").asText();

                StockNewsFlash flash = new StockNewsFlash();
                flash.setNewsId(newsId);
                flash.setTitle(title.length() > 500 ? title.substring(0, 500) : title);
                flash.setContent(content);
                flash.setPublishTime(item.path("ctime").asLong());
                flash.setCreateTime(LocalDateTime.now());
                newsFlashService.save(flash);
                saved++;
            }

            // 写 Redis：最新 30 条
            List<StockNewsFlash> dbList = newsFlashService.listLatest(30);
            List<StockNewsFlashVO> voList = dbList.stream().map(f -> StockNewsFlashVO.builder()
                    .id(f.getNewsId())
                    .title(f.getTitle())
                    .content(f.getContent())
                    .publishTime(f.getPublishTime())
                    .build()).collect(Collectors.toList());

            String json = objectMapper.writeValueAsString(voList);
            redisTemplate.opsForValue().set(REDIS_NEWSFLASH, json, Duration.ofSeconds(120));
            newsFlashL1.put("list", voList);
            log.info("NewsFlash: fetched={}, saved={}", rollData.size(), saved);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch newsFlashes failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 3. 热点题材 ====================

    public List<StockHotTopicVO> fetchAndCacheHotTopics() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            LocalDate today = LocalDate.now();
            String dateStr = today.toString();
            String url = "https://zx.10jqka.com.cn/event/api/getharden/date/" + dateStr
                    + "/orderby/date/orderway/desc/charset/GBK/";
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray()) return Collections.emptyList();

            // 从 API 响应中取实际交易日期（非交易日查询时返回最近交易日数据）
            String actualDate = root.path("date").asText();
            if (actualDate.isEmpty()) actualDate = dateStr;
            LocalDate tradeDate = LocalDate.parse(actualDate);

            // 先删当天旧数据，再批量 insert
            hotTopicService.deleteByDate(tradeDate);
            int saved = 0;
            for (JsonNode item : data) {
                StockHotTopic topic = new StockHotTopic();
                topic.setTopicDate(tradeDate);
                topic.setCode(item.path("code").asText());
                topic.setName(item.path("name").asText());
                topic.setReason(item.path("reason").asText());
                topic.setChangePct(toDecimal(item.path("zhangfu").asText()));
                topic.setTurnover(toDecimal(item.path("huanshou").asText()));
                topic.setVolume(item.path("chengjiaoe").asLong(0));
                topic.setClosePrice(toDecimal(item.path("close").asText()));
                topic.setMarket(item.path("market").asText());
                topic.setCreateTime(LocalDateTime.now());
                hotTopicService.save(topic);
                saved++;
            }

            // 写 Redis
            List<StockHotTopicVO> voList = new ArrayList<>();
            // 按 reason 聚合相似题材，去重
            Map<String, List<StockHotTopic>> grouped = new LinkedHashMap<>();
            for (JsonNode item : data) {
                String reason = item.path("reason").asText();
                if (reason.isEmpty()) reason = "其他";
                // 取第一个标签作为分组 key
                String key = reason.split("\\+")[0].trim();
                grouped.computeIfAbsent(key, k -> new ArrayList<>());
                // 简化存储
            }

            // 直接取前 20 条热门股作为展示
            int count = 0;
            for (JsonNode item : data) {
                if (count >= 20) break;
                String name = item.path("name").asText();
                String reason = item.path("reason").asText();
                if (name.isEmpty()) continue;
                StockHotTopicVO vo = StockHotTopicVO.builder()
                        .code(item.path("code").asText())
                        .name(name)
                        .reason(reason)
                        .changePct(toDecimal(item.path("zhangfu").asText()))
                        .stocks(Collections.singletonList(item.path("code").asText()))
                        .topicDate(actualDate)
                        .build();
                voList.add(vo);
                count++;
            }

            String json = objectMapper.writeValueAsString(voList);
            redisTemplate.opsForValue().set(REDIS_HOTTOPICS, json, Duration.ofSeconds(300));
            hotTopicsL1.put("list", voList);
            log.info("HotTopics: fetched={}, saved={}", data.size(), saved);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch hotTopics failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 4. 行业排名 ====================

    public List<StockIndustryRankVO> fetchAndCacheIndustryRank() {
        try {
            String url = "https://push2.eastmoney.com/api/qt/clist/get"
                    + "?pn=1&pz=100&po=1&np=1&fltt=2&invt=2"
                    + "&fs=m:90+t:2"
                    + "&fields=f2,f3,f4,f12,f13,f14,f104,f105,f128,f136,f140";
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode diff = root.path("data").path("diff");
            if (!diff.isArray()) return Collections.emptyList();

            LocalDateTime snapTime = LocalDateTime.now();
            int saved = 0;
            for (JsonNode item : diff) {
                StockIndustryRank rank = new StockIndustryRank();
                rank.setSnapTime(snapTime);
                rank.setCode(item.path("f12").asText());
                rank.setName(item.path("f14").asText());
                rank.setChangePct(toDecimal(item.path("f3").asText()));
                rank.setUpCount(item.path("f104").asInt());
                rank.setDownCount(item.path("f105").asInt());
                rank.setLeaderName(item.path("f140").asText());
                rank.setLeaderChange(toDecimal(item.path("f136").asText()));
                rank.setCreateTime(LocalDateTime.now());
                industryRankService.save(rank);
                saved++;
            }

            List<StockIndustryRankVO> voList = new ArrayList<>();
            for (JsonNode item : diff) {
                StockIndustryRankVO vo = StockIndustryRankVO.builder()
                        .code(item.path("f12").asText())
                        .name(item.path("f14").asText())
                        .changePct(toDecimal(item.path("f3").asText()))
                        .upCount(item.path("f104").asInt())
                        .downCount(item.path("f105").asInt())
                        .leaderName(item.path("f140").asText())
                        .leaderChange(toDecimal(item.path("f136").asText()))
                        .build();
                voList.add(vo);
            }

            String json = objectMapper.writeValueAsString(voList);
            redisTemplate.opsForValue().set(REDIS_INDUSTRY, json, Duration.ofSeconds(300));
            log.info("IndustryRank: fetched={}, saved={}", diff.size(), saved);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch industryRank failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 5. 北向资金 ====================

    public NorthboundFlowVO fetchAndCacheNorthboundFlow() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Host", "data.hexin.cn");
            headers.set("Referer", "https://data.hexin.cn/");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = "https://data.hexin.cn/market/hsgtApi/method/dayChart/";
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());

            JsonNode times = root.path("time");
            JsonNode hgtArr = root.path("hgt");
            JsonNode sgtArr = root.path("sgt");

            if (!hgtArr.isArray() || !sgtArr.isArray()) return null;

            BigDecimal hgt = BigDecimal.ZERO;
            BigDecimal sgt = BigDecimal.ZERO;
            if (hgtArr.size() > 0) hgt = new BigDecimal(hgtArr.get(hgtArr.size() - 1).asText());
            if (sgtArr.size() > 0) sgt = new BigDecimal(sgtArr.get(sgtArr.size() - 1).asText());

            String updateTime = "";
            if (times.isArray() && times.size() > 0) {
                updateTime = times.get(times.size() - 1).asText();
            }

            String tradeDate = getLatestTradingDay().toString();

            NorthboundFlowVO vo = NorthboundFlowVO.builder()
                    .hgt(hgt)
                    .sgt(sgt)
                    .total(hgt.add(sgt))
                    .updateTime(updateTime)
                    .tradeDate(tradeDate)
                    .build();

            String json = objectMapper.writeValueAsString(vo);
            redisTemplate.opsForValue().set(REDIS_NORTHBOUND, json, Duration.ofSeconds(90));
            return vo;
        } catch (Exception e) {
            log.warn("Fetch northbound flow failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 6. 融资融券 ====================

    public MarginTradingVO fetchAndCacheMarginTrading() {
        try {
            String url = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPTA_RZRQ_LSHJ"
                    + "&columns=ALL&pageSize=1"
                    + "&sortColumns=DIM_DATE&sortTypes=-1";
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.path("result").path("data");
            if (!data.isArray() || data.size() == 0) return null;

            JsonNode item = data.get(0);
            BigDecimal rzye = new BigDecimal(item.path("RZYE").asText())
                    .divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP);
            BigDecimal rqye = new BigDecimal(item.path("RQYE").asText())
                    .divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP);
            BigDecimal rzrqye = rzye.add(rqye);
            String tradeDate = item.path("DIM_DATE").asText();
            if (tradeDate.length() > 10) tradeDate = tradeDate.substring(0, 10);

            MarginTradingVO vo = MarginTradingVO.builder()
                    .rzye(rzye)
                    .rqye(rqye)
                    .rzrqye(rzrqye)
                    .tradeDate(tradeDate)
                    .build();

            String json = objectMapper.writeValueAsString(vo);
            redisTemplate.opsForValue().set(REDIS_MARGIN, json, Duration.ofSeconds(360));
            return vo;
        } catch (Exception e) {
            log.warn("Fetch margin trading failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 6b. 融资融券历史 ====================

    private static final String REDIS_MARGIN_HISTORY = "stock:margin:history";

    public List<MarginTradingHistoryVO> fetchAndCacheMarginHistory() {
        try {
            String url = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPTA_RZRQ_LSHJ"
                    + "&columns=ALL&pageSize=30"
                    + "&sortColumns=DIM_DATE&sortTypes=-1";
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.path("result").path("data");
            if (!data.isArray()) return Collections.emptyList();

            List<MarginTradingHistoryVO> list = new ArrayList<>();
            for (JsonNode item : data) {
                String tradeDate = item.path("DIM_DATE").asText();
                if (tradeDate.length() > 10) tradeDate = tradeDate.substring(0, 10);
                MarginTradingHistoryVO vo = MarginTradingHistoryVO.builder()
                        .tradeDate(tradeDate)
                        .rzye(new BigDecimal(item.path("RZYE").asText())
                                .divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP))
                        .rqye(new BigDecimal(item.path("RQYE").asText())
                                .divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP))
                        .rzrqye(new BigDecimal(item.path("RZYE").asText())
                                .add(new BigDecimal(item.path("RQYE").asText()))
                                .divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP))
                        .build();
                list.add(vo);
            }

            String json = objectMapper.writeValueAsString(list);
            redisTemplate.opsForValue().set(REDIS_MARGIN_HISTORY, json, Duration.ofSeconds(600));
            log.info("MarginHistory: fetched={}", list.size());
            return list;
        } catch (Exception e) {
            log.warn("Fetch margin history failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<MarginTradingHistoryVO> getMarginHistoryFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_MARGIN_HISTORY);
            if (json != null) {
                return Arrays.asList(objectMapper.readValue(json, MarginTradingHistoryVO[].class));
            }
        } catch (Exception e) {
            log.warn("Read margin history cache failed: {}", e.getMessage());
        }
        return fetchAndCacheMarginHistory();
    }

    // ==================== 7. 龙虎榜 ====================

    public List<StockDragonBoardVO> fetchAndCacheDragonBoard() {
        try {
            // 第一步：取最近一个交易日
            String latestUrl = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPT_DAILYBILLBOARD_DETAILSNEW"
                    + "&columns=TRADE_DATE&pageSize=1"
                    + "&sortColumns=TRADE_DATE&sortTypes=-1";
            String latestRaw = restTemplate.getForObject(latestUrl, String.class);
            JsonNode latestRoot = objectMapper.readTree(latestRaw);
            JsonNode latestData = latestRoot.path("result").path("data");
            String dateStr = null;
            if (latestData.isArray() && latestData.size() > 0) {
                dateStr = latestData.get(0).path("TRADE_DATE").asText();
            }
            if (dateStr == null || dateStr.length() < 10) {
                dateStr = LocalDate.now().toString();
            }
            dateStr = dateStr.substring(0, 10);
            LocalDate tradeDate = LocalDate.parse(dateStr);

            // 第二步：拉取该交易日全部龙虎榜数据（pageSize=100 覆盖全天~80条）
            String url = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPT_DAILYBILLBOARD_DETAILSNEW"
                    + "&columns=ALL&pageSize=100"
                    + "&sortColumns=BILLBOARD_NET_AMT&sortTypes=-1"
                    + "&filter=(TRADE_DATE='" + dateStr + "')";
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.path("result").path("data");
            if (!data.isArray()) return Collections.emptyList();

            dragonBoardService.deleteByTradeDate(tradeDate);
            int saved = 0;
            Set<String> seenCodes = new HashSet<>();
            for (JsonNode item : data) {
                String code = item.path("SECURITY_CODE").asText();
                String name = item.path("SECURITY_NAME_ABBR").asText();
                if (code.isEmpty() || !seenCodes.add(code)) continue;
                if (name.contains("ST") || name.contains("退")) continue; // 剔除ST和退市股
                StockDragonBoard board = new StockDragonBoard();
                board.setTradeDate(tradeDate);
                board.setCode(code);
                board.setName(name);
                board.setReason(item.path("EXPLANATION").asText());
                board.setClosePrice(toDecimal(item.path("CLOSE_PRICE").asText()));
                board.setChangePct(toDecimal(item.path("CHANGE_RATE").asText()));
                board.setNetBuy(toDecimal(item.path("BILLBOARD_NET_AMT").asText()));
                board.setBuyAmt(toDecimal(item.path("BILLBOARD_BUY_AMT").asText()));
                board.setSellAmt(toDecimal(item.path("BILLBOARD_SELL_AMT").asText()));
                board.setTurnoverPct(toDecimal(item.path("TURNOVERRATE").asText()));
                board.setCreateTime(LocalDateTime.now());
                dragonBoardService.save(board);
                saved++;
            }

            List<StockDragonBoardVO> voList = new ArrayList<>();
            Set<String> seenForVo = new HashSet<>();
            for (JsonNode item : data) {
                String code = item.path("SECURITY_CODE").asText();
                String name2 = item.path("SECURITY_NAME_ABBR").asText();
                if (code.isEmpty() || !seenForVo.add(code)) continue;
                if (name2.contains("ST") || name2.contains("退")) continue;
                StockDragonBoardVO vo = StockDragonBoardVO.builder()
                        .tradeDate(dateStr)
                        .code(code)
                        .name(name2)
                        .reason(item.path("EXPLANATION").asText())
                        .closePrice(toDecimal(item.path("CLOSE_PRICE").asText()))
                        .changePct(toDecimal(item.path("CHANGE_RATE").asText()))
                        .netBuy(toDecimal(item.path("BILLBOARD_NET_AMT").asText()))
                        .buyAmt(toDecimal(item.path("BILLBOARD_BUY_AMT").asText()))
                        .sellAmt(toDecimal(item.path("BILLBOARD_SELL_AMT").asText()))
                        .turnoverPct(toDecimal(item.path("TURNOVERRATE").asText()))
                        .build();
                voList.add(vo);
            }

            String json = objectMapper.writeValueAsString(voList);
            redisTemplate.opsForValue().set(REDIS_DRAGONBOARD, json, Duration.ofSeconds(180));
            dragonBoardL1.put("list", voList);
            log.info("DragonBoard: fetched={}, saved={}, tradeDate={}", data.size(), saved, tradeDate);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch dragonBoard failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 8. 限售解禁 ====================

    public List<StockLockupExpiryVO> fetchAndCacheLockupExpiry() {
        try {
            LocalDate today = LocalDate.now();
            String dateStr = today.toString();
            String url = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPT_LIFT_STAGE"
                    + "&columns=ALL&pageSize=30"
                    + "&sortColumns=FREE_DATE&sortTypes=1"
                    + "&filter=(FREE_DATE>='" + dateStr + "')";
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.path("result").path("data");
            if (!data.isArray()) return Collections.emptyList();

            lockupExpiryService.deleteAllFuture();
            int saved = 0;
            for (JsonNode item : data) {
                StockLockupExpiry lockup = new StockLockupExpiry();
                lockup.setCode(item.path("SECURITY_CODE").asText());
                lockup.setName(item.path("SECURITY_NAME_ABBR").asText());
                String freeDateStr = item.path("FREE_DATE").asText();
                if (freeDateStr.length() >= 10) {
                    lockup.setFreeDate(LocalDate.parse(freeDateStr.substring(0, 10)));
                }
                lockup.setLockupType(item.path("LIMITED_STOCK_TYPE").asText());
                lockup.setShares(toDecimal(item.path("FREE_SHARES_NUM").asText()));
                lockup.setRatio(toDecimal(item.path("FREE_RATIO").asText()));
                lockup.setCreateTime(LocalDateTime.now());
                lockupExpiryService.save(lockup);
                saved++;
            }

            List<StockLockupExpiryVO> voList = new ArrayList<>();
            for (JsonNode item : data) {
                String freeDateStr = item.path("FREE_DATE").asText();
                if (freeDateStr.length() >= 10) freeDateStr = freeDateStr.substring(0, 10);
                StockLockupExpiryVO vo = StockLockupExpiryVO.builder()
                        .code(item.path("SECURITY_CODE").asText())
                        .name(item.path("SECURITY_NAME_ABBR").asText())
                        .freeDate(freeDateStr)
                        .lockupType(item.path("LIMITED_STOCK_TYPE").asText())
                        .shares(toDecimal(item.path("FREE_SHARES_NUM").asText()))
                        .ratio(toDecimal(item.path("FREE_RATIO").asText()))
                        .build();
                voList.add(vo);
            }

            String json = objectMapper.writeValueAsString(voList);
            redisTemplate.opsForValue().set(REDIS_LOCKUP, json, Duration.ofSeconds(7200));
            log.info("LockupExpiry: fetched={}, saved={}", data.size(), saved);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch lockupExpiry failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 8b. 连板天梯 ====================

    /** 全A股代码列表（静态缓存，启动时生成一次） */
    private static final List<String> ALL_STOCK_CODES = generateAllStockCodes();

    private static List<String> generateAllStockCodes() {
        List<String> codes = new ArrayList<>(20000);
        // 沪市主板
        for (int i = 600000; i <= 605999; i++) codes.add("sh" + i);
        // 科创板
        for (int i = 688000; i <= 689999; i++) codes.add("sh" + i);
        // 深市主板+中小板
        for (int i = 1; i <= 4999; i++) codes.add("sz" + String.format("%06d", i));
        // 创业板
        for (int i = 300000; i <= 301999; i++) codes.add("sz" + i);
        // 北交所
        for (int i = 830000; i <= 839999; i++) codes.add("bj" + i);
        for (int i = 870000; i <= 879999; i++) codes.add("bj" + i);
        for (int i = 920000; i <= 929999; i++) codes.add("bj" + i);
        log.info("Generated {} potential stock codes for batch query", codes.size());
        return codes;
    }

    /** 获取A股市场前缀 */
    private String marketPrefix(String code) {
        if (code.startsWith("6")) return "sh";
        if (code.startsWith("8") || code.startsWith("92")) return "bj";
        return "sz";
    }

    /** 获取最近交易日 */
    private LocalDate getLatestTradingDay() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) return today.minusDays(1);
        if (dow == DayOfWeek.SUNDAY) return today.minusDays(2);
        return today;
    }

    /** 前一交易日 */
    private LocalDate previousTradingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.MONDAY) return date.minusDays(3);
        return date.minusDays(1);
    }

    public BoardLadderVO fetchAndCacheBoardLadder() {
        try {
            LocalDate tradingDay = getLatestTradingDay();
            String dateStr = tradingDay.toString();

            // 1. 通过腾讯财经批量查询全市场行情，筛选涨幅>=9.5%的涨停股
            List<JsonNode> limitUpList = fetchLimitUpStocksFromTencent();
            log.info("BoardLadder: tencent batch query found {} limit-up stocks", limitUpList.size());

            if (limitUpList.isEmpty()) {
                return buildEmptyLadder(dateStr);
            }

            // 3. 用30日K线计算每只股票的连板数
            List<String> codes = limitUpList.stream()
                    .map(i -> i.path("code").asText())
                    .collect(Collectors.toList());
            Map<String, Integer> klineBoardMap = fetchBoardCountsFromKline(codes);

            // 4. 构建结果
            List<StockBoardItemVO> allItems = new ArrayList<>();
            List<StockBoardDaily> todayEntities = new ArrayList<>();

            for (JsonNode item : limitUpList) {
                String code = item.path("code").asText();
                String name = item.path("name").asText();
                String reason = item.path("reason").asText();
                BigDecimal changePct = toDecimal(item.path("zhangfu").asText());
                BigDecimal closePrice = toDecimal(item.path("close").asText());
                BigDecimal turnover = toDecimal(item.path("huanshou").asText());

                int boardCount = klineBoardMap.getOrDefault(code, 1);

                allItems.add(StockBoardItemVO.builder()
                        .code(code).name(name).changePct(changePct)
                        .closePrice(closePrice).turnover(turnover)
                        .reason(reason).boardCount(boardCount).build());

                StockBoardDaily entity2 = new StockBoardDaily();
                entity2.setTradeDate(tradingDay); entity2.setCode(code);
                entity2.setName(name); entity2.setChangePct(changePct);
                entity2.setClosePrice(closePrice); entity2.setTurnover(turnover);
                entity2.setReason(reason); entity2.setBoardCount(boardCount);
                entity2.setCreateTime(LocalDateTime.now());
                todayEntities.add(entity2);
            }

            // 5. 存入DB
            boardService.deleteByDate(tradingDay);
            boardService.saveBatch(todayEntities);

            // 6. 分组
            List<StockBoardItemVO> firstBoard = allItems.stream()
                    .filter(i -> i.getBoardCount() == 1).collect(Collectors.toList());
            List<StockBoardItemVO> secondBoard = allItems.stream()
                    .filter(i -> i.getBoardCount() == 2).collect(Collectors.toList());
            List<StockBoardItemVO> thirdPlusBoard = allItems.stream()
                    .filter(i -> i.getBoardCount() >= 3).collect(Collectors.toList());

            BoardLadderVO vo = BoardLadderVO.builder()
                    .tradeDate(dateStr)
                    .firstBoard(firstBoard)
                    .secondBoard(secondBoard)
                    .thirdPlusBoard(thirdPlusBoard)
                    .totalCount(allItems.size())
                    .build();

            String json = objectMapper.writeValueAsString(vo);
            redisTemplate.opsForValue().set(REDIS_BOARDLADDER, json, Duration.ofSeconds(180));
            log.info("BoardLadder({}): fetched={}, 1st={}, 2nd={}, 3rd+={}",
                    dateStr, allItems.size(), firstBoard.size(), secondBoard.size(), thirdPlusBoard.size());
            return vo;
        } catch (Exception e) {
            log.warn("Fetch boardLadder failed: {}", e.getMessage(), e);
            return buildEmptyLadder(LocalDate.now().toString());
        }
    }

    /**
     * 通过腾讯财经K线接口并发获取每只股票的30日数据，计算连续涨停天数。
     * K线接口不支持批量查询，每只股票一个请求，通过线程池并发。
     */
    private Map<String, Integer> fetchBoardCountsFromKline(List<String> codes) {
        Map<String, Integer> result = new ConcurrentHashMap<>();
        if (codes.isEmpty()) return result;

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(8);
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

        for (String code : codes) {
            futures.add(executor.submit(() -> {
                try {
                    int boardCount = calcBoardCountFromKline(code);
                    result.put(code, boardCount);
                } catch (Exception e) {
                    result.put(code, 1);
                }
            }));
        }

        for (java.util.concurrent.Future<?> f : futures) {
            try { f.get(10, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();
        return result;
    }

    private int calcBoardCountFromKline(String code) {
        List<StockKlineVO> kline = getKline(code, 30);
        return calcConsecutiveLimitUps(kline);
    }

    /**
     * 从K线日数据（前复权）计算最近连续涨停天数。
     * changePct 为东财提供的涨跌幅（相对前一日收盘），当日已确认涨停，
     * 从倒数第2个开始往旧方向检查。
     */
    private int calcConsecutiveLimitUps(List<StockKlineVO> kline) {
        int count = 1; // 当日已确认涨停
        if (kline.size() < 2) return count;
        // 从倒数第二天开始，往前检查每一日是否涨停
        for (int i = kline.size() - 2; i >= 0; i--) {
            BigDecimal changePct = kline.get(i).getChangePct();
            if (changePct != null && changePct.compareTo(new BigDecimal("9.5")) >= 0) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * 腾讯财经全市场批量查询，筛选涨幅>=9.5%的涨停股。
     * 将全A股代码分200只/批，8线程并发查询，解析GBK行情数据。
     */
    private List<JsonNode> fetchLimitUpStocksFromTencent() {
        List<JsonNode> result = new ArrayList<>();
        int batchSize = 200;
        int totalCodes = ALL_STOCK_CODES.size();

        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<List<JsonNode>>> futures = new ArrayList<>();

        for (int i = 0; i < totalCodes; i += batchSize) {
            int end = Math.min(i + batchSize, totalCodes);
            List<String> batch = ALL_STOCK_CODES.subList(i, end);
            futures.add(executor.submit(() -> {
                List<JsonNode> batchResult = new ArrayList<>();
                try {
                    String url = "https://qt.gtimg.cn/q=" + String.join(",", batch);
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("User-Agent", "Mozilla/5.0");
                    HttpEntity<?> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                    if (resp.getBody() == null) return batchResult;

                    String[] lines = resp.getBody().split(";");
                    for (String line : lines) {
                        if (line.isEmpty() || !line.contains("=") || !line.contains("\"")) continue;
                        String val = line.split("\"")[1];
                        String[] f = val.split("~");
                        if (f.length < 53) continue;
                        BigDecimal changePct = toDecimal(f[32]);
                        if (changePct != null && changePct.compareTo(new BigDecimal("9.5")) >= 0) {
                            com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
                            node.put("code", f[2]);
                            node.put("name", f[1]);
                            node.put("zhangfu", f[32]);
                            node.put("close", f[3]);
                            node.put("huanshou", f[38]);
                            node.put("reason", "");
                            batchResult.add(node);
                        }
                    }
                } catch (Exception e) {
                    // 单个批次失败不中断，继续其他批次
                }
                return batchResult;
            }));
        }

        for (Future<List<JsonNode>> f : futures) {
            try {
                result.addAll(f.get(30, TimeUnit.SECONDS));
            } catch (Exception ignored) {}
        }
        executor.shutdown();
        return result;
    }

    private BoardLadderVO buildEmptyLadder(String dateStr) {
        return BoardLadderVO.builder()
                .tradeDate(dateStr)
                .firstBoard(Collections.emptyList())
                .secondBoard(Collections.emptyList())
                .thirdPlusBoard(Collections.emptyList())
                .totalCount(0).build();
    }

    public BoardLadderVO getBoardLadderFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_BOARDLADDER);
            if (json != null) {
                return objectMapper.readValue(json, BoardLadderVO.class);
            }
        } catch (Exception e) {
            log.warn("Read boardLadder cache failed: {}", e.getMessage());
        }
        return fetchAndCacheBoardLadder();
    }

    // ==================== 8c. 个股日K线（read-through缓存，供前端弹窗） ====================

    public List<StockKlineVO> getKline(String code) {
        return getKline(code, 30);
    }

    public List<StockKlineVO> getKline(String code, int days) {
        if (code == null || code.isEmpty()) return Collections.emptyList();
        String cacheKey = code + ":" + days;

        // L1: Caffeine
        List<StockKlineVO> l1Hit = klineL1.getIfPresent(cacheKey);
        if (l1Hit != null) return l1Hit;

        // L2: Redis
        try {
            String json = redisTemplate.opsForValue().get(REDIS_KLINE_PREFIX + cacheKey);
            if (json != null) {
                List<StockKlineVO> list = Arrays.asList(objectMapper.readValue(json, StockKlineVO[].class));
                klineL1.put(cacheKey, list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read kline L2 failed for {}: {}", code, e.getMessage());
        }

        // L3: 源（腾讯K线API）
        List<StockKlineVO> data = fetchSingleKlineData(code, days);
        if (!data.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(REDIS_KLINE_PREFIX + cacheKey, json, Duration.ofMinutes(30));
            } catch (Exception e) {
                log.warn("Write kline L2 failed for {}: {}", code, e.getMessage());
            }
            klineL1.put(cacheKey, data);
        }
        return data;
    }

    /**
     * 从新浪获取个股最近N日K线（前复权），返回完整日K线数据列表。
     * 返回 JSON 数组，元素字段: day, open, high, low, close, volume
     */
    private List<StockKlineVO> fetchSingleKlineData(String code, int days) {
        try {
            String url = "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol="
                    + marketPrefix(code) + code + "&scale=240&ma=no&datalen=" + days;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (!root.isArray() || root.size() == 0) return Collections.emptyList();

            List<StockKlineVO> result = new ArrayList<>();
            BigDecimal prevClose = null;
            for (JsonNode day : root) {
                BigDecimal open = toDecimal(day.path("open").asText());
                BigDecimal close = toDecimal(day.path("close").asText());
                BigDecimal high = toDecimal(day.path("high").asText());
                BigDecimal low = toDecimal(day.path("low").asText());

                BigDecimal changePct = null;
                if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) != 0) {
                    changePct = close.subtract(prevClose)
                            .divide(prevClose, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                }
                prevClose = close;

                result.add(StockKlineVO.builder()
                        .date(day.path("day").asText())
                        .open(open)
                        .close(close)
                        .high(high)
                        .low(low)
                        .volume(toDecimal(day.path("volume").asText()).longValue())
                        .changePct(changePct)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("Fetch kline data failed for {}: {}", code, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 8d. 股票名称字典 ====================

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getStockNames() {
        long now = System.currentTimeMillis();
        if (stockNamesCache != null && (now - stockNamesCacheTs) < 86400000L) {
            return (List<Map<String, Object>>) (List<?>) stockNamesCache;
        }
        List<Map<String, Object>> names = jdbcTemplate.queryForList(
                "SELECT code, REPLACE(name, ' ', '') AS name FROM da_win10.da_01_stock_basic ORDER BY CHAR_LENGTH(name) DESC");
        stockNamesCache = (List<Map<String, String>>) (List<?>) names;
        stockNamesCacheTs = now;
        return names;
    }

    // ==================== 9. 东财全球资讯 ====================

    public List<StockGlobalNewsVO> fetchAndCacheGlobalNews() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Referer", "https://kuaixun.eastmoney.com/");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = "https://np-weblist.eastmoney.com/comm/web/getFastNewsList"
                    + "?client=web&biz=web_724&fastColumn=102&pageSize=50"
                    + "&sortEnd=&req_trace=" + System.currentTimeMillis();
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode newsList = root.path("data").path("fastNewsList");
            if (!newsList.isArray()) return Collections.emptyList();

            int saved = 0;
            for (JsonNode item : newsList) {
                String newsCode = item.path("code").asText();
                if (newsCode.isEmpty() || globalNewsService.existsByNewsCode(newsCode)) continue;

                StockGlobalNews news = new StockGlobalNews();
                news.setNewsCode(newsCode);
                news.setTitle(item.path("title").asText());
                String summary = item.path("summary").asText();
                news.setSummary(summary.length() > 500 ? summary.substring(0, 500) : summary);
                news.setShowTime(item.path("showTime").asText());
                news.setCreateTime(LocalDateTime.now());
                globalNewsService.save(news);
                saved++;
            }

            List<StockGlobalNews> dbList = globalNewsService.listLatest(30);
            List<StockGlobalNewsVO> voList = dbList.stream().map(n -> StockGlobalNewsVO.builder()
                    .newsCode(n.getNewsCode())
                    .title(n.getTitle())
                    .summary(n.getSummary())
                    .showTime(n.getShowTime())
                    .build()).collect(Collectors.toList());

            String json = objectMapper.writeValueAsString(voList);
            redisTemplate.opsForValue().set(REDIS_GLOBALNEWS, json, Duration.ofSeconds(180));
            log.info("GlobalNews: fetched={}, saved={}", newsList.size(), saved);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch globalNews failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 读缓存（read-through） ====================

    public List<StockIndexVO> getIndicesFromCache() {
        // L1: Caffeine
        List<StockIndexVO> l1Hit = indicesL1.getIfPresent("list");
        if (l1Hit != null) return l1Hit;

        // L2: Redis
        try {
            String json = redisTemplate.opsForValue().get(REDIS_INDICES);
            if (json != null) {
                List<StockIndexVO> list = Arrays.asList(objectMapper.readValue(json, StockIndexVO[].class));
                indicesL1.put("list", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read indices cache failed: {}", e.getMessage());
        }
        List<StockIndexVO> list = fetchAndCacheIndices();
        if (list != null) indicesL1.put("list", list);
        return list;
    }

    public List<StockNewsFlashVO> getNewsFlashesFromCache() {
        // L1: Caffeine
        List<StockNewsFlashVO> l1Hit = newsFlashL1.getIfPresent("list");
        if (l1Hit != null) return l1Hit;

        // L2: Redis
        try {
            String json = redisTemplate.opsForValue().get(REDIS_NEWSFLASH);
            if (json != null) {
                List<StockNewsFlashVO> list = Arrays.asList(objectMapper.readValue(json, StockNewsFlashVO[].class));
                newsFlashL1.put("list", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read newsFlash L2 failed: {}", e.getMessage());
        }

        // L3: MySQL
        List<StockNewsFlash> dbList = newsFlashService.listLatest(30);
        List<StockNewsFlashVO> voList = dbList.stream().map(f -> StockNewsFlashVO.builder()
                .id(f.getNewsId())
                .title(f.getTitle())
                .content(f.getContent())
                .publishTime(f.getPublishTime())
                .build()).collect(Collectors.toList());
        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_NEWSFLASH, json, Duration.ofSeconds(120));
            } catch (Exception e) {
                log.warn("Backfill newsFlash L2 failed: {}", e.getMessage());
            }
            newsFlashL1.put("list", voList);
        }
        return voList;
    }

    public List<StockHotTopicVO> getHotTopicsFromCache() {
        // L1: Caffeine
        List<StockHotTopicVO> l1Hit = hotTopicsL1.getIfPresent("list");
        if (l1Hit != null) return l1Hit;

        // L2: Redis
        try {
            String json = redisTemplate.opsForValue().get(REDIS_HOTTOPICS);
            if (json != null) {
                List<StockHotTopicVO> list = Arrays.asList(objectMapper.readValue(json, StockHotTopicVO[].class));
                hotTopicsL1.put("list", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read hotTopics L2 failed: {}", e.getMessage());
        }

        // L3: MySQL
        List<StockHotTopic> dbList = hotTopicService.listLatest();
        List<StockHotTopicVO> voList = dbList.stream().map(t -> StockHotTopicVO.builder()
                .code(t.getCode())
                .name(t.getName())
                .reason(t.getReason())
                .changePct(t.getChangePct())
                .stocks(Collections.singletonList(t.getCode()))
                .topicDate(t.getTopicDate() != null ? t.getTopicDate().toString() : "")
                .build()).collect(Collectors.toList());
        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_HOTTOPICS, json, Duration.ofSeconds(300));
            } catch (Exception e) {
                log.warn("Backfill hotTopics L2 failed: {}", e.getMessage());
            }
            hotTopicsL1.put("list", voList);
        }
        return voList;
    }

    // ==================== 11. 股东户数变化 ====================

    public List<StockHolderNumVO> fetchAndCacheHolderNum() {
        try {
            String url = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPT_HOLDERNUMLATEST"
                    + "&columns=ALL&pageSize=500"
                    + "&sortColumns=HOLDER_NUM_RATIO&sortTypes=1";
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.path("result").path("data");
            if (!data.isArray()) return Collections.emptyList();

            // 取最新报告期
            String latestDate = "";
            for (JsonNode item : data) {
                String d = item.path("END_DATE").asText();
                if (d.compareTo(latestDate) > 0) latestDate = d;
            }
            if (latestDate.length() >= 10) latestDate = latestDate.substring(0, 10);
            LocalDate endDate = LocalDate.parse(latestDate);

            // 删旧写新
            holderNumService.deleteByEndDate(endDate);

            // 第一遍：收集符合条件的股票代码
            List<JsonNode> filteredItems = new ArrayList<>();
            for (JsonNode item : data) {
                String ed = item.path("END_DATE").asText();
                if (ed.length() >= 10) ed = ed.substring(0, 10);
                if (!ed.equals(latestDate)) continue;

                BigDecimal ratio = toDecimal(item.path("HOLDER_NUM_RATIO").asText());
                if (ratio == null || ratio.compareTo(BigDecimal.ZERO) >= 0) continue;

                if (filteredItems.size() >= 100) break;
                filteredItems.add(item);
            }

            // 顺序查询行业/地区（emweb F10 API）
            Map<String, String[]> infoMap = new LinkedHashMap<>();
            for (JsonNode item : filteredItems) {
                String code = item.path("SECURITY_CODE").asText();
                try {
                    String prefix = code.startsWith("6") || code.startsWith("9") || code.startsWith("8") ? "SH" : "SZ";
                    String f10Url = "https://emweb.securities.eastmoney.com/PC_HSF10/CompanySurvey/CompanySurveyAjax?code=" + prefix + code;
                    String f10Raw = restTemplate.getForObject(f10Url, String.class);
                    JsonNode node = objectMapper.readTree(f10Raw);
                    JsonNode jbzl = node.path("jbzl");
                    infoMap.put(code, new String[]{
                        jbzl.path("sshy").asText(),
                        jbzl.path("qy").asText()
                    });
                    Thread.sleep(300); // 避免频率过高
                } catch (Exception e) {
                    log.warn("F10 stock info fail {}: {}", code, e.getMessage());
                }
            }

            List<StockHolderNum> entities = new ArrayList<>();
            List<StockHolderNumVO> voList = new ArrayList<>();
            for (JsonNode item : filteredItems) {
                String code = item.path("SECURITY_CODE").asText();
                BigDecimal ratio = toDecimal(item.path("HOLDER_NUM_RATIO").asText());
                String[] info = infoMap.getOrDefault(code, new String[]{"", ""});

                StockHolderNum entity = new StockHolderNum();
                entity.setEndDate(endDate);
                entity.setCode(code);
                entity.setName(item.path("SECURITY_NAME_ABBR").asText());
                entity.setIndustry(info[0]);
                entity.setRegion(info[1]);
                entity.setHolderNum(item.path("HOLDER_NUM").asLong());
                entity.setChangeNum(item.path("HOLDER_NUM_CHANGE").asLong());
                entity.setChangeRatio(ratio);
                entity.setAvgShares(toDecimal(item.path("AVG_FREE_SHARES").asText()));
                entity.setCreateTime(LocalDateTime.now());
                entities.add(entity);

                StockHolderNumVO vo = StockHolderNumVO.builder()
                        .endDate(latestDate)
                        .code(entity.getCode())
                        .name(entity.getName())
                        .industry(entity.getIndustry())
                        .region(entity.getRegion())
                        .holderNum(entity.getHolderNum())
                        .changeNum(entity.getChangeNum())
                        .changeRatio(entity.getChangeRatio())
                        .avgShares(entity.getAvgShares())
                        .build();
                voList.add(vo);
            }

            if (!entities.isEmpty()) {
                holderNumService.saveBatch(entities);
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_HOLDERNUM, json, Duration.ofSeconds(600));
                holderNumL1.put("list", voList);
            }
            log.info("HolderNum: saved={}, date={}", entities.size(), latestDate);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch holderNum failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<StockHolderNumVO> getHolderNumFromCache() {
        // L1: Caffeine
        List<StockHolderNumVO> l1Hit = holderNumL1.getIfPresent("list");
        if (l1Hit != null) return l1Hit;

        // L2: Redis
        try {
            String json = redisTemplate.opsForValue().get(REDIS_HOLDERNUM);
            if (json != null) {
                List<StockHolderNumVO> list = Arrays.asList(objectMapper.readValue(json, StockHolderNumVO[].class));
                holderNumL1.put("list", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read holderNum L2 failed: {}", e.getMessage());
        }

        // L3: MySQL
        List<StockHolderNum> dbList = holderNumService.listLatest(100);
        List<StockHolderNumVO> voList = dbList.stream().map(e -> StockHolderNumVO.builder()
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : "")
                .code(e.getCode())
                .name(e.getName())
                .industry(e.getIndustry())
                .region(e.getRegion())
                .holderNum(e.getHolderNum())
                .changeNum(e.getChangeNum())
                .changeRatio(e.getChangeRatio())
                .avgShares(e.getAvgShares())
                .build()).collect(Collectors.toList());
        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_HOLDERNUM, json, Duration.ofSeconds(600));
            } catch (Exception ex) {
                log.warn("Backfill holderNum L2 failed: {}", ex.getMessage());
            }
            holderNumL1.put("list", voList);
        }
        return voList;
    }

    // ==================== 11b. 十大流通股东 ====================

    private volatile boolean holderTop10Fetching;

    public List<StockHolderTop10VO> fetchAndCacheHolderTop10() {
        if (holderTop10Fetching) {
            return getHolderTop10FromCacheDirect();
        }
        holderTop10Fetching = true;
        try {
            // 1. 获取最新报告期
            String dateUrl = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPT_F10_EH_HOLDERS"
                    + "&columns=END_DATE&pageSize=1"
                    + "&sortColumns=END_DATE&sortTypes=-1"
                    + "&filter=(SHARES_TYPE=\"流通A股\")";
            String dateRaw = restTemplate.getForObject(dateUrl, String.class);
            JsonNode dateRoot = objectMapper.readTree(dateRaw);
            JsonNode dateData = dateRoot.path("result").path("data");
            String latestDate = "";
            if (dateData.isArray() && dateData.size() > 0) {
                String rawDate = dateData.get(0).path("END_DATE").asText();
                if (rawDate.length() >= 10) latestDate = rawDate.substring(0, 10);
            }
            if (latestDate.isEmpty()) {
                log.warn("HolderTop10: no END_DATE found");
                return Collections.emptyList();
            }
            final String reportDate = latestDate;

            // 2. 清空旧数据（全量刷新，先删再写避免主键冲突）
            holderTop10Service.deleteAll();

            // 3. 分页拉取，每只股票只保留最新报告期数据（API 按 END_DATE DESC 排序）
            int saved = 0;
            Set<String> seenCodes = new HashSet<>();
            Set<String> seenKeys = new HashSet<>(); // dedup: "code-date-rank"
            List<StockHolderTop10> batch = new ArrayList<>();

            for (int page = 1; page <= 200; page++) {
                String url = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                        + "?reportName=RPT_F10_EH_HOLDERS"
                        + "&columns=ALL&pageSize=500&pageIndex=" + page
                        + "&sortColumns=END_DATE&sortTypes=-1"
                        + "&filter=(SHARES_TYPE=\"流通A股\")";
                String raw = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(raw);
                JsonNode data = root.path("result").path("data");
                if (!data.isArray() || data.size() == 0) break;

                for (JsonNode item : data) {
                    String code = item.path("SECURITY_CODE").asText();
                    if (code.isEmpty() || seenCodes.contains(code)) continue;
                    int rank = item.path("HOLDER_RANK").asInt();
                    if (rank > 10) continue;

                    String ed = item.path("END_DATE").asText();
                    if (ed.length() >= 10) ed = ed.substring(0, 10);
                    String key = code + "-" + ed + "-" + rank;
                    if (!seenKeys.add(key)) continue;

                    StockHolderTop10 entity = new StockHolderTop10();
                    entity.setEndDate(LocalDate.parse(ed));
                    entity.setCode(code);
                    entity.setName(item.path("SECURITY_NAME_ABBR").asText());
                    entity.setHolderName(item.path("HOLDER_NAME").asText());
                    entity.setHoldNum(item.path("HOLD_NUM").asLong());
                    entity.setHoldRatio(toDecimal(item.path("HOLD_NUM_RATIO").asText()));
                    entity.setHoldChange(item.path("HOLD_NUM_CHANGE").asText());
                    entity.setHolderRank(rank);
                    entity.setSharesType(item.path("SHARES_TYPE").asText());
                    entity.setCreateTime(LocalDateTime.now());
                    batch.add(entity);
                }

                if (!batch.isEmpty()) {
                    holderTop10Service.saveBatch(batch);
                    saved += batch.size();
                    batch.clear();
                }
                for (JsonNode item : data) {
                    String code = item.path("SECURITY_CODE").asText();
                    if (!code.isEmpty()) seenCodes.add(code);
                }
                if (data.size() < 500) break;
            }

            // 4. 写缓存
            List<StockHolderTop10> dbList = holderTop10Service.listLatest();
            List<StockHolderTop10VO> voList = dbList.stream().map(e -> StockHolderTop10VO.builder()
                    .endDate(e.getEndDate() != null ? e.getEndDate().toString() : "")
                    .code(e.getCode())
                    .name(e.getName())
                    .holderName(e.getHolderName())
                    .holdNum(e.getHoldNum())
                    .holdRatio(e.getHoldRatio())
                    .holdChange(e.getHoldChange())
                    .holderRank(e.getHolderRank())
                    .sharesType(e.getSharesType())
                    .build()).collect(Collectors.toList());

            if (!voList.isEmpty()) {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_HOLDERTOP10, json, Duration.ofHours(2));
                holderTop10L1.put("list", voList);
            }
            log.info("HolderTop10: saved={}, stocks={}, latestDate={}", saved, seenCodes.size(), reportDate);
            return voList;
        } catch (Exception e) {
            log.warn("Fetch holderTop10 failed: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            holderTop10Fetching = false;
        }
    }

    private List<StockHolderTop10VO> getHolderTop10FromCacheDirect() {
        List<StockHolderTop10VO> l1Hit = holderTop10L1.getIfPresent("list");
        if (l1Hit != null) return l1Hit;
        try {
            String json = redisTemplate.opsForValue().get(REDIS_HOLDERTOP10);
            if (json != null) {
                List<StockHolderTop10VO> list = Arrays.asList(objectMapper.readValue(json, StockHolderTop10VO[].class));
                holderTop10L1.put("list", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read holderTop10 L2 failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<StockHolderTop10VO> getHolderTop10FromCache() {
        List<StockHolderTop10VO> l1Hit = holderTop10L1.getIfPresent("list");
        if (l1Hit != null) return l1Hit;

        try {
            String json = redisTemplate.opsForValue().get(REDIS_HOLDERTOP10);
            if (json != null) {
                List<StockHolderTop10VO> list = Arrays.asList(
                        objectMapper.readValue(json, StockHolderTop10VO[].class));
                holderTop10L1.put("list", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read holderTop10 L2 failed: {}", e.getMessage());
        }

        // L3: MySQL — 取最新报告期数据
        List<StockHolderTop10> dbList = holderTop10Service.listLatest();
        List<StockHolderTop10VO> voList = dbList.stream().map(e -> StockHolderTop10VO.builder()
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : "")
                .code(e.getCode())
                .name(e.getName())
                .holderName(e.getHolderName())
                .holdNum(e.getHoldNum())
                .holdRatio(e.getHoldRatio())
                .holdChange(e.getHoldChange())
                .holderRank(e.getHolderRank())
                .sharesType(e.getSharesType())
                .build()).collect(Collectors.toList());

        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_HOLDERTOP10, json, Duration.ofHours(2));
            } catch (Exception ex) {
                log.warn("Backfill holderTop10 L2 failed: {}", ex.getMessage());
            }
            holderTop10L1.put("list", voList);
        }
        return voList;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            // 从 Redis 取行业排名计算涨跌家数
            String json = redisTemplate.opsForValue().get(REDIS_INDUSTRY);
            if (json == null) {
                List<StockIndustryRankVO> ranks = fetchAndCacheIndustryRank();
                int upSum = ranks.stream().mapToInt(r -> r.getUpCount() != null ? r.getUpCount() : 0).sum();
                int downSum = ranks.stream().mapToInt(r -> r.getDownCount() != null ? r.getDownCount() : 0).sum();
                stats.put("upCount", upSum);
                stats.put("downCount", downSum);
            } else {
                StockIndustryRankVO[] ranks = objectMapper.readValue(json, StockIndustryRankVO[].class);
                int upSum = Arrays.stream(ranks).mapToInt(r -> r.getUpCount() != null ? r.getUpCount() : 0).sum();
                int downSum = Arrays.stream(ranks).mapToInt(r -> r.getDownCount() != null ? r.getDownCount() : 0).sum();
                stats.put("upCount", upSum);
                stats.put("downCount", downSum);
            }
        } catch (Exception e) {
            log.warn("Compute stats failed: {}", e.getMessage());
            stats.put("upCount", 0);
            stats.put("downCount", 0);
        }
        return stats;
    }

    // ==================== 新增 read-through ====================

    public NorthboundFlowVO getNorthboundFlowFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_NORTHBOUND);
            if (json != null) {
                return objectMapper.readValue(json, NorthboundFlowVO.class);
            }
        } catch (Exception e) {
            log.warn("Read northbound cache failed: {}", e.getMessage());
        }
        return fetchAndCacheNorthboundFlow();
    }

    public MarginTradingVO getMarginTradingFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_MARGIN);
            if (json != null) {
                return objectMapper.readValue(json, MarginTradingVO.class);
            }
        } catch (Exception e) {
            log.warn("Read margin cache failed: {}", e.getMessage());
        }
        return fetchAndCacheMarginTrading();
    }

    public List<StockDragonBoardVO> getDragonBoardFromCache() {
        // L1: Caffeine 本地缓存
        List<StockDragonBoardVO> l1Hit = dragonBoardL1.getIfPresent("list");
        if (l1Hit != null) {
            return l1Hit;
        }

        // L2: Redis 分布式缓存
        try {
            String json = redisTemplate.opsForValue().get(REDIS_DRAGONBOARD);
            if (json != null) {
                List<StockDragonBoardVO> list = Arrays.asList(objectMapper.readValue(json, StockDragonBoardVO[].class));
                dragonBoardL1.put("list", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read dragonBoard L2 failed: {}", e.getMessage());
        }

        // L3: MySQL
        List<StockDragonBoard> dbList = dragonBoardService.listLatest(30);
        List<StockDragonBoardVO> voList = dbList.stream().map(b -> StockDragonBoardVO.builder()
                .tradeDate(b.getTradeDate() != null ? b.getTradeDate().toString() : "")
                .code(b.getCode())
                .name(b.getName())
                .reason(b.getReason())
                .closePrice(b.getClosePrice())
                .changePct(b.getChangePct())
                .netBuy(b.getNetBuy())
                .buyAmt(b.getBuyAmt())
                .sellAmt(b.getSellAmt())
                .turnoverPct(b.getTurnoverPct())
                .build()).collect(Collectors.toList());
        if (!voList.isEmpty()) {
            // 回填 L2 + L1
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_DRAGONBOARD, json, Duration.ofSeconds(180));
            } catch (Exception e) {
                log.warn("Backfill dragonBoard L2 failed: {}", e.getMessage());
            }
            dragonBoardL1.put("list", voList);
        }
        return voList;
    }

    public List<StockLockupExpiryVO> getLockupExpiryFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_LOCKUP);
            if (json != null) {
                return Arrays.asList(objectMapper.readValue(json, StockLockupExpiryVO[].class));
            }
        } catch (Exception e) {
            log.warn("Read lockup cache failed: {}", e.getMessage());
        }
        List<StockLockupExpiry> dbList = lockupExpiryService.listUpcoming(30);
        List<StockLockupExpiryVO> voList = dbList.stream().map(l -> StockLockupExpiryVO.builder()
                .code(l.getCode())
                .name(l.getName())
                .freeDate(l.getFreeDate() != null ? l.getFreeDate().toString() : "")
                .lockupType(l.getLockupType())
                .shares(l.getShares())
                .ratio(l.getRatio())
                .build()).collect(Collectors.toList());
        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_LOCKUP, json, Duration.ofSeconds(7200));
            } catch (Exception e) {
                log.warn("Backfill lockup cache failed: {}", e.getMessage());
            }
        }
        return voList;
    }

    public List<StockGlobalNewsVO> getGlobalNewsFromCache() {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_GLOBALNEWS);
            if (json != null) {
                return Arrays.asList(objectMapper.readValue(json, StockGlobalNewsVO[].class));
            }
        } catch (Exception e) {
            log.warn("Read globalNews cache failed: {}", e.getMessage());
        }
        List<StockGlobalNews> dbList = globalNewsService.listLatest(30);
        List<StockGlobalNewsVO> voList = dbList.stream().map(n -> StockGlobalNewsVO.builder()
                .newsCode(n.getNewsCode())
                .title(n.getTitle())
                .summary(n.getSummary())
                .showTime(n.getShowTime())
                .build()).collect(Collectors.toList());
        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_GLOBALNEWS, json, Duration.ofSeconds(180));
            } catch (Exception e) {
                log.warn("Backfill globalNews cache failed: {}", e.getMessage());
            }
        }
        return voList;
    }

    // ==================== 12. 优质公司三标准评分 ====================

    public List<StockQualityScoreVO> fetchAndCacheQualityScores() {
        try {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();

            // 1. 按年抓取财务数据并分批写入 DB（避免全量数据 OOM）
            stockFinancialService.deleteAll();
            int totalRecords = fetchAndSaveFinancialData(currentYear - 5, currentYear);
            if (totalRecords == 0) {
                log.warn("QualityScore: no financial data fetched");
                return Collections.emptyList();
            }
            log.info("QualityScore: fetched {} financial records", totalRecords);

            // 2. 从 DB 分批读取，逐批评分
            List<StockFinancial> allFinancials = stockFinancialService.list();
            Map<String, List<StockFinancial>> grouped = allFinancials.stream()
                    .collect(Collectors.groupingBy(StockFinancial::getCode));

            List<StockQualityScore> scores = new ArrayList<>();
            List<StockQualityScoreVO> vos = new ArrayList<>();

            for (Map.Entry<String, List<StockFinancial>> entry : grouped.entrySet()) {
                String code = entry.getKey();
                List<StockFinancial> financials = entry.getValue();
                if (financials.isEmpty()) continue;

                String name = financials.get(0).getName();
                financials.sort(Comparator.comparingInt(StockFinancial::getReportYear));

                // 条件一: 主营业务收入同比不出现 >20% 下滑 (最多1年例外)
                boolean revenuePass = checkRevenueNoCollapse(financials);

                // 条件二: 扣非净利润连续为正
                boolean profitPass = checkDeductedProfitPositive(financials);

                // 计算比率
                BigDecimal cashRevRatio = calcAvgCashRevenueRatio(financials);
                BigDecimal cashProfitRatio = calcAvgCashProfitRatio(financials);

                // 条件三: 经营现金流/营收 > 0.5
                boolean cashRevPass = cashRevRatio != null
                        && cashRevRatio.compareTo(new BigDecimal("0.5")) > 0;

                // 条件四: 经营现金流/净利润 > 1
                boolean cashProfitPass = cashProfitRatio != null
                        && cashProfitRatio.compareTo(BigDecimal.ONE) > 0;

                // 加权评分
                BigDecimal score = BigDecimal.ZERO;
                if (revenuePass) score = score.add(new BigDecimal("0.20"));
                if (profitPass) score = score.add(new BigDecimal("0.30"));
                if (cashRevPass) score = score.add(new BigDecimal("0.20"));
                if (cashProfitPass) score = score.add(new BigDecimal("0.30"));

                String detailJson = buildDetailJson(financials);

                StockQualityScore entity = new StockQualityScore();
                entity.setCode(code);
                entity.setName(name);
                entity.setScore(score);
                entity.setRevenuePass(revenuePass ? 1 : 0);
                entity.setCashRevenueRatio(cashRevRatio);
                entity.setProfitPass(profitPass ? 1 : 0);
                entity.setCashProfitRatio(cashProfitRatio);
                entity.setDetailJson(detailJson);
                entity.setTradeDate(today);
                entity.setCreateTime(LocalDateTime.now());
                scores.add(entity);
            }

            enrichIndustries(scores);

            for (StockQualityScore entity : scores) {
                StockQualityScoreVO vo = StockQualityScoreVO.builder()
                        .code(entity.getCode()).name(entity.getName())
                        .industry(entity.getIndustry())
                        .score(entity.getScore())
                        .revenuePass(entity.getRevenuePass())
                        .cashRevenueRatio(entity.getCashRevenueRatio())
                        .profitPass(entity.getProfitPass())
                        .cashProfitRatio(entity.getCashProfitRatio())
                        .detailJson(entity.getDetailJson())
                        .build();
                vos.add(vo);
            }

            stockQualityScoreService.deleteAll();
            stockQualityScoreService.saveBatch(scores);

            vos.sort((a, b) -> b.getScore().compareTo(a.getScore()));
            List<StockQualityScoreVO> top100 = vos.size() > 100 ? vos.subList(0, 100) : vos;

            String json = objectMapper.writeValueAsString(top100);
            redisTemplate.opsForValue().set(REDIS_QUALITY, json, Duration.ofHours(6));
            qualityL1.put("top100", top100);

            log.info("QualityScore: scored {} stocks, top score={}",
                    scores.size(), top100.isEmpty() ? "N/A" : top100.get(0).getScore());
            return top100;
        } catch (Exception e) {
            log.warn("Fetch quality scores failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private boolean checkRevenueNoCollapse(List<StockFinancial> financials) {
        int violations = 0;
        for (int i = 1; i < financials.size(); i++) {
            BigDecimal prev = financials.get(i - 1).getMainRevenue();
            BigDecimal curr = financials.get(i).getMainRevenue();
            if (prev == null || curr == null || prev.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal change = curr.subtract(prev)
                    .divide(prev, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (change.compareTo(new BigDecimal("-20")) < 0) {
                violations++;
            }
        }
        return violations <= 1;
    }

    private boolean checkDeductedProfitPositive(List<StockFinancial> financials) {
        for (StockFinancial f : financials) {
            if (f.getDeductedNetProfit() == null
                    || f.getDeductedNetProfit().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
        }
        return !financials.isEmpty();
    }

    private BigDecimal calcAvgCashRevenueRatio(List<StockFinancial> financials) {
        BigDecimal sumRevenue = BigDecimal.ZERO;
        BigDecimal sumCash = BigDecimal.ZERO;
        for (StockFinancial f : financials) {
            if (f.getMainRevenue() != null) sumRevenue = sumRevenue.add(f.getMainRevenue());
            if (f.getOperatingCashFlow() != null) sumCash = sumCash.add(f.getOperatingCashFlow());
        }
        if (sumRevenue.compareTo(BigDecimal.ZERO) == 0) return null;
        return sumCash.divide(sumRevenue, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcAvgCashProfitRatio(List<StockFinancial> financials) {
        BigDecimal sumProfit = BigDecimal.ZERO;
        BigDecimal sumCash = BigDecimal.ZERO;
        for (StockFinancial f : financials) {
            if (f.getNetProfit() != null) sumProfit = sumProfit.add(f.getNetProfit());
            if (f.getOperatingCashFlow() != null) sumCash = sumCash.add(f.getOperatingCashFlow());
        }
        if (sumProfit.compareTo(BigDecimal.ZERO) == 0) return null;
        return sumCash.divide(sumProfit, 4, RoundingMode.HALF_UP);
    }

    private String buildDetailJson(List<StockFinancial> financials) {
        try {
            List<Map<String, Object>> detail = new ArrayList<>();
            for (StockFinancial f : financials) {
                Map<String, Object> yearData = new LinkedHashMap<>();
                yearData.put("year", f.getReportYear());
                yearData.put("mainRevenue", f.getMainRevenue());
                yearData.put("deductedNetProfit", f.getDeductedNetProfit());
                yearData.put("netProfit", f.getNetProfit());
                yearData.put("operatingCashFlow", f.getOperatingCashFlow());
                detail.add(yearData);
            }
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 从东财 datacenter RPT_LICO_FN_CPD 按年拉取年报财务数据，每页处理完直接写 DB 避免 OOM。
     * 返回写入总记录数。
     */
    private int fetchAndSaveFinancialData(int yearFrom, int yearTo) {
        int totalRecords = 0;
        for (int year = yearFrom; year <= yearTo; year++) {
            String filter = "(REPORTDATE='" + year + "-12-31')";
            int yearRecords = 0;
            List<StockFinancial> batch = new ArrayList<>();
            Set<String> seenCodeYear = new HashSet<>();

            for (int page = 1; ; page++) {
                String url = "https://datacenter-web.eastmoney.com/api/data/v1/get"
                        + "?reportName=RPT_LICO_FN_CPD"
                        + "&columns=SECURITY_CODE,SECURITY_NAME_ABBR,REPORTDATE,"
                        + "TOTAL_OPERATE_INCOME,PARENT_NETPROFIT,"
                        + "BASIC_EPS,DEDUCT_BASIC_EPS,MGJYXJJE"
                        + "&pageSize=500"
                        + "&pageIndex=" + page
                        + "&filter=" + filter;
                try {
                    String raw = restTemplate.getForObject(url, String.class);
                    JsonNode root = objectMapper.readTree(raw);
                    JsonNode data = root.path("result").path("data");
                    if (!data.isArray() || data.size() == 0) break;

                    for (JsonNode item : data) {
                        String code = item.path("SECURITY_CODE").asText();
                        String key = code + "-" + year;
                        if (!seenCodeYear.add(key)) continue;

                        StockFinancial sf = parseFinancialItem(item, year);
                        batch.add(sf);
                        yearRecords++;

                        // 每 500 条批量写入，释放内存
                        if (batch.size() >= 500) {
                            stockFinancialService.saveBatch(batch);
                            batch.clear();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Fetch financial year {} page {} failed: {}", year, page, e.getMessage());
                    break;
                }
            }
            // 写剩余数据
            if (!batch.isEmpty()) {
                stockFinancialService.saveBatch(batch);
                batch.clear();
            }
            totalRecords += yearRecords;
            log.info("Financial data: year={}, records={}", year, yearRecords);
        }
        return totalRecords;
    }

    private StockFinancial parseFinancialItem(JsonNode item, int year) {
        BigDecimal netProfit = toDecimalNull(item.path("PARENT_NETPROFIT").asText());
        BigDecimal basicEps = toDecimalNull(item.path("BASIC_EPS").asText());
        BigDecimal deductedEps = toDecimalNull(item.path("DEDUCT_BASIC_EPS").asText());
        BigDecimal mgjyxjje = toDecimalNull(item.path("MGJYXJJE").asText());

        BigDecimal totalShares = null;
        if (netProfit != null && basicEps != null
                && basicEps.compareTo(BigDecimal.ZERO) != 0) {
            totalShares = netProfit.divide(basicEps, 0, RoundingMode.HALF_UP).abs();
        }

        StockFinancial sf = new StockFinancial();
        sf.setCode(item.path("SECURITY_CODE").asText());
        sf.setName(item.path("SECURITY_NAME_ABBR").asText());
        sf.setReportYear(year);
        sf.setMainRevenue(toDecimalNull(item.path("TOTAL_OPERATE_INCOME").asText()));
        sf.setNetProfit(netProfit);
        if (deductedEps != null && totalShares != null) {
            sf.setDeductedNetProfit(deductedEps.multiply(totalShares));
        }
        if (mgjyxjje != null && totalShares != null) {
            sf.setOperatingCashFlow(mgjyxjje.multiply(totalShares));
        }
        sf.setCreateTime(LocalDateTime.now());
        return sf;
    }

    private BigDecimal toDecimalNull(String s) {
        try {
            if (s == null || s.isEmpty() || s.equals("-")) return null;
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void enrichIndustries(List<StockQualityScore> scores) {
        for (StockQualityScore s : scores) {
            try {
                String code = s.getCode();
                String prefix = code.startsWith("6") || code.startsWith("9") || code.startsWith("8") ? "SH" : "SZ";
                String f10Url = "https://emweb.securities.eastmoney.com/PC_HSF10/CompanySurvey/CompanySurveyAjax?code=" + prefix + code;
                String f10Raw = restTemplate.getForObject(f10Url, String.class);
                JsonNode node = objectMapper.readTree(f10Raw);
                JsonNode jbzl = node.path("jbzl");
                s.setIndustry(jbzl.path("sshy").asText());
                Thread.sleep(300);
            } catch (Exception e) {
                log.warn("Enrich industry fail {}: {}", s.getCode(), e.getMessage());
            }
        }
    }

    public List<StockQualityScoreVO> getQualityScoresFromCache() {
        List<StockQualityScoreVO> l1Hit = qualityL1.getIfPresent("top100");
        if (l1Hit != null) return l1Hit;

        try {
            String json = redisTemplate.opsForValue().get(REDIS_QUALITY);
            if (json != null) {
                List<StockQualityScoreVO> list = Arrays.asList(
                        objectMapper.readValue(json, StockQualityScoreVO[].class));
                qualityL1.put("top100", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("Read quality L2 failed: {}", e.getMessage());
        }

        List<StockQualityScore> dbList = stockQualityScoreService.listLatest();
        List<StockQualityScoreVO> voList = dbList.stream().map(e -> StockQualityScoreVO.builder()
                .code(e.getCode()).name(e.getName()).industry(e.getIndustry())
                .score(e.getScore()).revenuePass(e.getRevenuePass())
                .cashRevenueRatio(e.getCashRevenueRatio()).profitPass(e.getProfitPass())
                .cashProfitRatio(e.getCashProfitRatio()).detailJson(e.getDetailJson())
                .build()).collect(Collectors.toList());

        if (!voList.isEmpty()) {
            voList.sort((a, b) -> b.getScore().compareTo(a.getScore()));
            List<StockQualityScoreVO> top100 = voList.size() > 100 ? voList.subList(0, 100) : voList;
            try {
                String json = objectMapper.writeValueAsString(top100);
                redisTemplate.opsForValue().set(REDIS_QUALITY, json, Duration.ofHours(6));
            } catch (Exception ex) {
                log.warn("Backfill quality L2 failed: {}", ex.getMessage());
            }
            qualityL1.put("top100", top100);
            return top100;
        }
        return Collections.emptyList();
    }

    // ==================== 13. 个股日成交数据（收盘后采集） ====================

    @org.springframework.transaction.annotation.Transactional
    public List<StockDailyAmountVO> fetchAndCacheDailyAmounts() {
        LocalDate tradeDate = getLatestTradingDay();
        log.info("DailyAmount: start collecting for {}", tradeDate);

        // 1. 分类数据（行业/概念/地区）——先走缓存，未命中调腾讯 stockinfo API
        Map<String, String[]> classMap = fetchStockClassifications(ALL_STOCK_CODES);
        log.info("DailyAmount: classification map size={}", classMap.size());

        // 2. 腾讯 qt.gtimg.cn 批量查询全A股，提取成交额/换手率/量比/流通市值，合并分类数据
        List<StockDailyAmount> entities = collectAllMarketData(tradeDate, classMap);
        log.info("DailyAmount: collected {} valid stock records", entities.size());

        // 3. 事务内：先删当日旧数据，再批量写入（去重+事务保护）
        dailyAmountService.deleteByTradeDate(tradeDate);
        if (!entities.isEmpty()) {
            dailyAmountService.saveBatch(entities);
        }

        // 3b. 计算今年上榜次数(top100)，更新本日所有股票
        Map<String, Integer> boardCounts = computeAllBoardCounts();
        for (StockDailyAmount e : entities) {
            Integer bc = boardCounts.get(e.getCode());
            if (bc != null && bc > 0) {
                e.setTop100Cs(bc);
            }
        }
        dailyAmountService.updateBatchById(entities);

        // 4. 查前一交易日数据用于 prevAmount（无数据则往前倒推最多5天）
        Map<String, BigDecimal> prevAmountMap = new HashMap<>();
        LocalDate prevTradeDate = previousTradingDay(tradeDate);
        for (int retry = 0; retry < 5; retry++) {
            List<StockDailyAmount> prevList = dailyAmountService.listByDate(prevTradeDate);
            if (!prevList.isEmpty()) {
                for (StockDailyAmount p : prevList) {
                    prevAmountMap.put(p.getCode(), p.getAmount());
                }
                break;
            }
            prevTradeDate = previousTradingDay(prevTradeDate);
        }

        List<StockDailyAmountVO> voList = new ArrayList<>();
        for (StockDailyAmount e : entities) {
            BigDecimal prevAmount = prevAmountMap.get(e.getCode());
            voList.add(mapToVO(e, prevAmount));
        }

        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_DAILY_AMOUNT + ":latest", json, Duration.ofHours(4));
            } catch (Exception ex) {
                log.warn("DailyAmount: write Redis failed: {}", ex.getMessage());
            }
            dailyAmountL1.put("latest", voList);
        }

        log.info("DailyAmount: done, {} records for {}", voList.size(), tradeDate);
        return voList;
    }

    /**
     * 东财 push2 一次请求获取全A股行业/概念/地区板块分类。
     * 返回 Map<code, [name, industry, concept, region]>
     */
    /**
     * 获取全A股行业/地区/概念分类。先查缓存，缓存未命中则调腾讯 stockinfo API。
     * 返回 Map<code, [name, industry, concept, region]>
     */
    private Map<String, String[]> fetchStockClassifications(List<String> prefixedCodes) {
        Map<String, String[]> map = new LinkedHashMap<>();
        // 统一使用裸码（无 sh/sz/bj 前缀）
        for (String pcode : prefixedCodes) {
            String code = stripPrefix(pcode);
            String[] cached = stockClassCache.get(code);
            if (cached != null) {
                map.put(code, cached);
            }
        }
        // 未命中的调用腾讯 stockinfo API（20线程并发）
        List<String> missing = new ArrayList<>();
        for (String pcode : prefixedCodes) {
            String code = stripPrefix(pcode);
            if (!map.containsKey(code)) missing.add(code);
        }
        if (!missing.isEmpty()) {
            log.info("DailyAmount: fetching stock info for {} uncached codes", missing.size());
            ExecutorService exec = Executors.newFixedThreadPool(10);
            List<Future<?>> futures = new ArrayList<>();
            for (String code : missing) {
                futures.add(exec.submit(() -> {
                    try {
                        String[] info = fetchSingleStockInfo(code);
                        if (info != null) {
                            synchronized (map) {
                                stockClassCache.put(code, info);
                                map.put(code, info);
                            }
                        }
                    } catch (Exception ignored) {}
                }));
            }
            for (Future<?> f : futures) {
                try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
            }
            exec.shutdown();
        }
        log.info("DailyAmount: resolved {} stock classifications", map.size());
        return map;
    }

    /** 调用腾讯 stockinfo/jiankuang API，返回 [name, industry, concept, region] */
    private String[] fetchSingleStockInfo(String code) {
        try {
            String prefix = marketPrefix(code);
            String url = "https://ifzq.gtimg.cn/appstock/app/stockinfo/jiankuang?code=" + prefix + code;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode gsjj = root.path("data").path("gsjj");
            if (gsjj.isMissingNode()) return null;

            String name = gsjj.path("gsmz").asText();
            String region = gsjj.path("dy").asText();
            // 行业(二级): plate level=2
            String industry = "";
            JsonNode plates = gsjj.path("plate");
            if (plates.isArray()) {
                for (JsonNode p : plates) {
                    if ("2".equals(p.path("level").asText())) {
                        industry = p.path("name").asText();
                        break;
                    }
                }
            }
            // 概念: 逗号拼接
            StringBuilder concepts = new StringBuilder();
            JsonNode conceptArr = gsjj.path("concept");
            if (conceptArr.isArray()) {
                for (JsonNode c : conceptArr) {
                    if (concepts.length() > 0) concepts.append(",");
                    concepts.append(c.path("name").asText());
                }
            }
            return new String[]{
                    name != null ? name.replace(" ", "") : "",
                    industry,
                    concepts.toString(),
                    region
            };
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 复用现有腾讯行情批量查询模式（200只/批 × 8线程），提取
     * f[1]=名称 f[2]=代码 f[37]=成交额(万) f[38]=换手率% f[45]=流通市值(亿) f[49]=量比
     * 并与 push2 分类数据合并。
     */
    private List<StockDailyAmount> collectAllMarketData(LocalDate tradeDate, Map<String, String[]> classMap) {
        List<StockDailyAmount> result = new ArrayList<>();
        int batchSize = 200;
        int totalCodes = ALL_STOCK_CODES.size();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<List<StockDailyAmount>>> futures = new ArrayList<>();

        for (int i = 0; i < totalCodes; i += batchSize) {
            int end = Math.min(i + batchSize, totalCodes);
            List<String> batch = ALL_STOCK_CODES.subList(i, end);
            futures.add(executor.submit(() -> {
                List<StockDailyAmount> batchResult = new ArrayList<>();
                try {
                    String url = "https://qt.gtimg.cn/q=" + String.join(",", batch);
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("User-Agent", "Mozilla/5.0");
                    HttpEntity<?> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                    if (resp.getBody() == null) return batchResult;

                    String[] lines = resp.getBody().split(";");
                    for (String line : lines) {
                        if (line.isEmpty() || !line.contains("=") || !line.contains("\"")) continue;
                        String val = line.split("\"")[1];
                        String[] f = val.split("~");
                        if (f.length < 53) continue;

                        String code = f[2];
                        if (code.isEmpty()) continue;

                        BigDecimal amount = toDecimalNull(f[37]);
                        // 跳过停牌/无成交的股票
                        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) continue;

                        BigDecimal turnoverPct = toDecimalNull(f[38]);
                        BigDecimal floatMarketCap = toDecimalNull(f[45]);
                        BigDecimal volumeRatio = toDecimalNull(f[49]);
                        String name = f[1];

                        StockDailyAmount sda = new StockDailyAmount();
                        sda.setTradeDate(tradeDate);
                        sda.setCode(code);
                        sda.setName(name);
                        sda.setAmount(amount);
                        sda.setTurnoverPct(turnoverPct);
                        sda.setVolumeRatio(volumeRatio);
                        sda.setFloatMarketCap(floatMarketCap);
                        sda.setCreateTime(LocalDateTime.now());

                        // 合并 push2 分类数据
                        String[] classInfo = classMap.get(code);
                        if (classInfo != null) {
                            if (classInfo[1] != null && !classInfo[1].isEmpty()) sda.setIndustry(classInfo[1]);
                            if (classInfo[2] != null && !classInfo[2].isEmpty()) sda.setConcept(classInfo[2]);
                            if (classInfo[3] != null && !classInfo[3].isEmpty()) sda.setRegion(classInfo[3]);
                        }

                        batchResult.add(sda);
                    }
                } catch (Exception e) {
                    // 单批次失败不中断
                }
                return batchResult;
            }));
        }

        for (Future<List<StockDailyAmount>> f : futures) {
            try {
                result.addAll(f.get(30, TimeUnit.SECONDS));
            } catch (Exception ignored) {}
        }
        executor.shutdown();

        // 按 code 排序，方便后续计算 prevAmount
        result.sort(Comparator.comparing(StockDailyAmount::getCode));
        return result;
    }

    /** 读缓存：取某只股票最近 N 个交易日的日成交数据 */
    public List<StockDailyAmountVO> getDailyAmountList(String code, int days) {
        if (code == null || code.isEmpty()) return Collections.emptyList();
        String cacheKey = code + ":" + days;

        // L1: Caffeine
        List<StockDailyAmountVO> l1Hit = dailyAmountL1.getIfPresent(cacheKey);
        if (l1Hit != null) return l1Hit;

        // L2: Redis
        try {
            String json = redisTemplate.opsForValue().get(REDIS_DAILY_AMOUNT + ":" + cacheKey);
            if (json != null) {
                List<StockDailyAmountVO> list = Arrays.asList(objectMapper.readValue(json, StockDailyAmountVO[].class));
                dailyAmountL1.put(cacheKey, list);
                return list;
            }
        } catch (Exception e) {
            log.warn("DailyAmount: read L2 failed for {}: {}", cacheKey, e.getMessage());
        }

        // L3: MySQL
        List<StockDailyAmount> dbList = dailyAmountService.listByCode(code, days);
        List<StockDailyAmountVO> voList = new ArrayList<>();
        for (int i = 0; i < dbList.size(); i++) {
            StockDailyAmount current = dbList.get(i);
            BigDecimal prevAmount = (i + 1 < dbList.size()) ? dbList.get(i + 1).getAmount() : null;
            voList.add(mapToVO(current, prevAmount));
        }

        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_DAILY_AMOUNT + ":" + cacheKey, json, Duration.ofMinutes(30));
            } catch (Exception e) {
                log.warn("DailyAmount: write L2 failed for {}: {}", cacheKey, e.getMessage());
            }
            dailyAmountL1.put(cacheKey, voList);
        }
        return voList;
    }

    /** 0点刷新展示表：取最新交易日 top200（top100CS已在15:30计算好）→ 写入展示表 */
    public List<StockDailyAmountVO> refreshDailyDisplay() {
        LocalDate latest = getLatestTradingDay();
        List<StockDailyAmount> dbList = dailyAmountService.listByDate(latest);
        if (dbList.isEmpty()) {
            latest = previousTradingDay(latest);
            dbList = dailyAmountService.listByDate(latest);
        }
        if (dbList.isEmpty()) return Collections.emptyList();

        // 按金额降序取 top200
        dbList.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        dbList = dbList.size() > 200 ? dbList.subList(0, 200) : dbList;

        // 写入展示表（清旧写新）
        List<StockDailyDisplay> displayList = new ArrayList<>();
        for (StockDailyAmount e : dbList) {
            StockDailyDisplay d = new StockDailyDisplay();
            d.setTradeDate(e.getTradeDate()); d.setCode(e.getCode()); d.setName(e.getName());
            d.setAmount(e.getAmount()); d.setTurnoverPct(e.getTurnoverPct());
            d.setVolumeRatio(e.getVolumeRatio()); d.setFloatMarketCap(e.getFloatMarketCap());
            d.setIndustry(e.getIndustry()); d.setConcept(e.getConcept()); d.setRegion(e.getRegion());
            d.setBoardCount(e.getTop100Cs() != null ? e.getTop100Cs() : 0);
            d.setCreateTime(LocalDateTime.now());
            displayList.add(d);
        }
        dailyDisplayService.replaceAll(displayList);

        // Redis L2 + L1
        List<StockDailyAmountVO> voList = new ArrayList<>();
        for (StockDailyDisplay e : displayList) {
            voList.add(toDisplayVO(e));
        }
        try {
            redisTemplate.opsForValue().set(REDIS_DAILY_DISPLAY,
                    objectMapper.writeValueAsString(voList), Duration.ofHours(25));
        } catch (Exception ex) {
            log.warn("DailyDisplay: write Redis L2 failed: {}", ex.getMessage());
        }
        dailyDisplayL1.put("display", voList);
        log.info("DailyDisplay: top200 refreshed for {}", latest);
        return voList;
    }

    private StockDailyAmountVO toDisplayVO(StockDailyDisplay e) {
        return StockDailyAmountVO.builder()
                .tradeDate(e.getTradeDate() != null ? e.getTradeDate().toString() : "")
                .code(e.getCode()).name(e.getName())
                .amount(e.getAmount()).turnoverPct(e.getTurnoverPct())
                .volumeRatio(e.getVolumeRatio()).floatMarketCap(e.getFloatMarketCap())
                .industry(e.getIndustry()).concept(e.getConcept()).region(e.getRegion())
                .boardCount(e.getBoardCount())
                .build();
    }

    /** 全量计算今年内每只股票进入成交额前100的天数 */
    private Map<String, Integer> computeAllBoardCounts() {
        // L2: Redis 缓存
        try {
            String json = redisTemplate.opsForValue().get(REDIS_BOARD_COUNT);
            if (json != null) {
                return objectMapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {});
            }
        } catch (Exception e) {
            log.warn("BoardCount: read Redis failed: {}", e.getMessage());
        }
        // L3: da_03_kline 窗口函数
        Map<String, Integer> map = new HashMap<>();
        try {
            String sql = "SELECT code, COUNT(*) AS cnt FROM (" +
                    " SELECT code, ROW_NUMBER() OVER (PARTITION BY trade_date ORDER BY amount DESC) AS rk" +
                    " FROM da_win10.da_03_kline" +
                    " WHERE trade_date >= '2026-01-01' AND amount > 0" +
                    ") t WHERE rk <= 100 GROUP BY code";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                String code = (String) row.get("code");
                Object cnt = row.get("cnt");
                if (code != null && cnt != null) {
                    map.put(code, ((Number) cnt).intValue());
                }
            }
            redisTemplate.opsForValue().set(REDIS_BOARD_COUNT,
                    objectMapper.writeValueAsString(map), Duration.ofDays(2));
        } catch (Exception e) {
            log.warn("BoardCount: compute failed: {}", e.getMessage());
        }
        log.info("BoardCount: computed {} stocks", map.size());
        return map;
    }

    /** 读取展示数据（三级缓存：Caffeine → Redis → MySQL展示表） */
    public List<StockDailyAmountVO> getDailyDisplay() {
        List<StockDailyAmountVO> l1Hit = dailyDisplayL1.getIfPresent("display");
        if (l1Hit != null) return l1Hit;

        try {
            String json = redisTemplate.opsForValue().get(REDIS_DAILY_DISPLAY);
            if (json != null) {
                List<StockDailyAmountVO> list = Arrays.asList(objectMapper.readValue(json, StockDailyAmountVO[].class));
                dailyDisplayL1.put("display", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("DailyDisplay: read L2 failed: {}", e.getMessage());
        }

        List<StockDailyDisplay> list = dailyDisplayService.list();
        if (list.isEmpty()) return refreshDailyDisplay();

        list.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        List<StockDailyAmountVO> voList = new ArrayList<>();
        for (StockDailyDisplay e : list) voList.add(toDisplayVO(e));
        return voList;
    }

    /** 取最新全量日成交数据（缓存优先） */
    public List<StockDailyAmountVO> getLatestDailyAmounts() {
        List<StockDailyAmountVO> l1Hit = dailyAmountL1.getIfPresent("latest");
        if (l1Hit != null) return l1Hit;

        try {
            String json = redisTemplate.opsForValue().get(REDIS_DAILY_AMOUNT + ":latest");
            if (json != null) {
                List<StockDailyAmountVO> list = Arrays.asList(objectMapper.readValue(json, StockDailyAmountVO[].class));
                dailyAmountL1.put("latest", list);
                return list;
            }
        } catch (Exception e) {
            log.warn("DailyAmount: read latest L2 failed: {}", e.getMessage());
        }

        // L3: MySQL — 当天 + 前一交易日，按 code 匹配 prevAmount
        LocalDate tradeDate = getLatestTradingDay();
        List<StockDailyAmount> todayList = dailyAmountService.listByDate(tradeDate);

        // 找前一交易日
        LocalDate prevDate = previousTradingDay(tradeDate);
        List<StockDailyAmount> prevList = dailyAmountService.listByDate(prevDate);
        // 如果数据还未采集，退到最近可用的历史日期
        if (prevList.isEmpty()) {
            prevDate = prevDate.minusDays(1);
            prevList = dailyAmountService.listByDate(prevDate);
        }

        Map<String, BigDecimal> prevAmountMap = new HashMap<>();
        for (StockDailyAmount p : prevList) {
            prevAmountMap.put(p.getCode(), p.getAmount());
        }

        List<StockDailyAmountVO> voList = new ArrayList<>();
        for (StockDailyAmount current : todayList) {
            BigDecimal prevAmount = prevAmountMap.get(current.getCode());
            voList.add(mapToVO(current, prevAmount));
        }

        if (!voList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(voList);
                redisTemplate.opsForValue().set(REDIS_DAILY_AMOUNT + ":latest", json, Duration.ofHours(4));
            } catch (Exception e) {
                log.warn("DailyAmount: backfill latest L2 failed: {}", e.getMessage());
            }
            dailyAmountL1.put("latest", voList);
        }
        return voList;
    }

    private StockDailyAmountVO mapToVO(StockDailyAmount e, BigDecimal prevAmount) {
        return StockDailyAmountVO.builder()
                .tradeDate(e.getTradeDate() != null ? e.getTradeDate().toString() : "")
                .code(e.getCode())
                .name(e.getName())
                .amount(e.getAmount())
                .turnoverPct(e.getTurnoverPct())
                .volumeRatio(e.getVolumeRatio())
                .floatMarketCap(e.getFloatMarketCap())
                .industry(e.getIndustry())
                .concept(e.getConcept())
                .region(e.getRegion())
                .prevAmount(prevAmount)
                .build();
    }

    private String stripPrefix(String code) {
        if (code == null) return null;
        return code.replaceFirst("^(sh|sz|bj)", "");
    }

    private String trimDash(String s) {
        if (s == null || s.isEmpty() || s.equals("-")) return "";
        return s;
    }

    private BigDecimal toDecimal(String s) {
        try {
            if (s == null || s.isEmpty() || s.equals("-")) return BigDecimal.ZERO;
            return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
