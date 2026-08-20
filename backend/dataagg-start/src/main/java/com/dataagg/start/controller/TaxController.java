package com.dataagg.start.controller;

import com.dataagg.common.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tax")
public class TaxController {

    private static final String REDIS_KEY = "tax:international";
    private static final int REDIS_TTL_HOURS = 6;

    private static final String[][] COUNTRIES = {
        {"CN", "中国"}, {"US", "美国"}, {"JP", "日本"}, {"DE", "德国"},
        {"GB", "英国"}, {"FR", "法国"}, {"KR", "韩国"}, {"IN", "印度"},
        {"BR", "巴西"}, {"RU", "俄罗斯"}
    };

    private static final String WB_API = "https://api.worldbank.org/v2/country/%s/indicator/GC.TAX.TOTL.GD.ZS?format=json&per_page=5";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Logger log = LoggerFactory.getLogger(TaxController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/international")
    public Result<?> getInternationalTax() {
        log.info("Tax international API called");
        try {
            // Redis 缓存
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null && !cached.isEmpty()) {
                List<Object> list = mapper.readValue(cached,
                    mapper.getTypeFactory().constructCollectionType(List.class, Object.class));
                return Result.success(list);
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (String[] c : COUNTRIES) {
                Map<String, Object> item = fetchCountry(c[0], c[1]);
                if (item != null) list.add(item);
            }

            // 按税率降序
            list.sort((a, b) -> Double.compare(
                (Double) b.get("taxRate"), (Double) a.get("taxRate")));

            log.info("Tax data fetched: {} countries", list.size());

            if (!list.isEmpty()) {
                redisTemplate.opsForValue().set(REDIS_KEY,
                    mapper.writeValueAsString(list), Duration.ofHours(REDIS_TTL_HOURS));
            }

            return Result.success(list);
        } catch (Exception e) {
            log.error("Tax international fetch failed", e);
            return Result.error("获取税收数据失败: " + e.getMessage());
        }
    }

    private Map<String, Object> fetchCountry(String code, String name) {
        try {
            String url = String.format(WB_API, code);
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode arr = mapper.readTree(raw);

            if (!arr.isArray() || arr.size() < 2 || arr.get(1).size() == 0) {
                return null;
            }

            // 跳过 null 值，取有效数据
            Double value = null, prevValue = null;
            int year = 0, prevYear = 0;
            for (JsonNode node : arr.get(1)) {
                if (node.path("value").isNull()) continue;
                if (value == null) {
                    value = node.path("value").asDouble();
                    year = node.path("date").asInt();
                } else if (prevValue == null) {
                    prevValue = node.path("value").asDouble();
                    prevYear = node.path("date").asInt();
                    break;
                }
            }
            if (value == null) return null;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("code", code);
            item.put("taxRate", Math.round(value * 100.0) / 100.0);
            item.put("year", year);
            item.put("prevRate", prevValue != null ? Math.round(prevValue * 100.0) / 100.0 : null);
            item.put("prevYear", prevYear > 0 ? prevYear : null);
            item.put("change", prevValue != null
                ? Math.round((value - prevValue) * 100.0) / 100.0 : 0.0);
            return item;
        } catch (Exception e) {
            log.warn("Fetch tax for {} failed: {}", code, e.getMessage());
            return null;
        }
    }
}
