package com.dataagg.start.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

@Component
public class MultiLevelCache {

    private static final Logger log = LoggerFactory.getLogger(MultiLevelCache.class);
    private static final String REDIS_PREFIX = "cache:";
    private static final Duration DEFAULT_L1_TTL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_L2_TTL = Duration.ofMinutes(10);

    private final Map<String, Duration> l2Ttls = new ConcurrentHashMap<>();
    private final Map<String, Duration> l1Ttls = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // L1 本地缓存：按缓存名独立 TTL，避免每次请求都跨网络读 Redis
    private final Cache<String, Object> l1Cache;

    // 按 cacheName 统计 L1 命中/未命中
    private final Map<String, LongAdder> l1Hits = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> l1Misses = new ConcurrentHashMap<>();

    public MultiLevelCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(500)
                .recordStats()
                .expireAfter(new Expiry<String, Object>() {
                    @Override
                    public long expireAfterCreate(String key, Object value, long currentTime) {
                        String cacheName = cacheNameFromKey(key);
                        return l1Ttls.getOrDefault(cacheName, DEFAULT_L1_TTL).toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, Object value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, Object value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    private static String cacheNameFromKey(String l1Key) {
        int idx = l1Key.indexOf(':');
        return idx > 0 ? l1Key.substring(0, idx) : l1Key;
    }

    public void register(String cacheName, Duration l2Ttl) {
        register(cacheName, DEFAULT_L1_TTL, l2Ttl);
    }

    public void register(String cacheName, Duration l1Ttl, Duration l2Ttl) {
        l1Ttls.put(cacheName, l1Ttl);
        l2Ttls.put(cacheName, l2Ttl);
        log.info("Cache [{}] registered: L1={} L2={}", cacheName, l1Ttl, l2Ttl);
    }

    private Duration l2Ttl(String cacheName) {
        return l2Ttls.getOrDefault(cacheName, DEFAULT_L2_TTL);
    }

    private String l1Key(String cacheName, String key) {
        return cacheName + ":" + key;
    }

    private void recordHit(String cacheName) {
        l1Hits.computeIfAbsent(cacheName, k -> new LongAdder()).increment();
    }

    private void recordMiss(String cacheName) {
        l1Misses.computeIfAbsent(cacheName, k -> new LongAdder()).increment();
    }

    public <T> T get(String cacheName, String key, Class<T> type, Supplier<T> dbLoader) {
        String l1k = l1Key(cacheName, key);
        String redisKey = REDIS_PREFIX + cacheName + ":" + key;

        boolean l1Hit = l1Cache.getIfPresent(l1k) != null;
        if (l1Hit) recordHit(cacheName); else recordMiss(cacheName);

        try {
            return type.cast(l1Cache.get(l1k, k -> {
                // L2: Redis
                try {
                    String json = redis.opsForValue().get(redisKey);
                    if (json != null && !json.isEmpty()) {
                        log.debug("L2 hit [{}:{}]", cacheName, key);
                        return objectMapper.readValue(json, type);
                    }
                } catch (Exception e) {
                    log.warn("L2 read error [{}:{}]: {}", cacheName, key, e.getMessage());
                }

                // DB
                log.debug("Cache miss [{}:{}], loading from DB", cacheName, key);
                T dbResult = dbLoader.get();
                if (dbResult != null) {
                    try {
                        String json = objectMapper.writeValueAsString(dbResult);
                        Duration t = l2Ttl(cacheName);
                        redis.opsForValue().set(redisKey, json, t.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.warn("Cache write error [{}:{}]: {}", cacheName, key, e.getMessage());
                    }
                }
                return dbResult;
            }));
        } catch (Exception e) {
            log.warn("Cache load error [{}:{}]: {}", cacheName, key, e.getMessage());
            return dbLoader.get();
        }
    }

    public <T> T get(String cacheName, String key, JavaType type, Supplier<T> dbLoader) {
        String l1k = l1Key(cacheName, key);
        String redisKey = REDIS_PREFIX + cacheName + ":" + key;

        boolean l1Hit = l1Cache.getIfPresent(l1k) != null;
        if (l1Hit) recordHit(cacheName); else recordMiss(cacheName);

        try {
            @SuppressWarnings("unchecked")
            T result = (T) l1Cache.get(l1k, k -> {
                // L2: Redis
                try {
                    String json = redis.opsForValue().get(redisKey);
                    if (json != null && !json.isEmpty()) {
                        log.debug("L2 hit [{}:{}]", cacheName, key);
                        return objectMapper.readValue(json, type);
                    }
                } catch (Exception e) {
                    log.warn("L2 read error [{}:{}]: {}", cacheName, key, e.getMessage());
                }

                // DB
                log.debug("Cache miss [{}:{}], loading from DB", cacheName, key);
                T dbResult = dbLoader.get();
                if (dbResult != null) {
                    try {
                        String json = objectMapper.writeValueAsString(dbResult);
                        Duration t = l2Ttl(cacheName);
                        redis.opsForValue().set(redisKey, json, t.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.warn("Cache write error [{}:{}]: {}", cacheName, key, e.getMessage());
                    }
                }
                return dbResult;
            });
            return result;
        } catch (Exception e) {
            log.warn("Cache load error [{}:{}]: {}", cacheName, key, e.getMessage());
            return dbLoader.get();
        }
    }

    public void evict(String cacheName, String key) {
        String redisKey = REDIS_PREFIX + cacheName + ":" + key;
        try {
            redis.delete(redisKey);
        } catch (Exception e) {
            log.warn("Cache evict error [{}:{}]: {}", cacheName, key, e.getMessage());
        }
        l1Cache.invalidate(l1Key(cacheName, key));
        log.debug("Evicted [{}:{}]", cacheName, key);
    }

    public void evictAll(String cacheName) {
        String pattern = REDIS_PREFIX + cacheName + ":*";
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<byte[]> cursor = redis.getConnectionFactory().getConnection().scan(options)) {
                while (cursor.hasNext()) {
                    redis.delete(new String(cursor.next()));
                }
            }
        } catch (Exception e) {
            log.warn("Cache evictAll error [{}]: {}", cacheName, e.getMessage());
        }
        l1Cache.asMap().keySet().removeIf(k -> k.startsWith(cacheName + ":"));
        log.debug("Evicted all [{}]", cacheName);
    }

    public Map<String, Map<String, Object>> getStats() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String name : l2Ttls.keySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("l1_ttl", l1Ttls.getOrDefault(name, DEFAULT_L1_TTL).toString());
            m.put("l2_ttl", l2Ttls.get(name).toString());
            m.put("type", "L1(Caffeine)+L2(Redis)");
            // 实际 L1 命中率统计
            long hits = l1Hits.getOrDefault(name, new LongAdder()).sum();
            long misses = l1Misses.getOrDefault(name, new LongAdder()).sum();
            long total = hits + misses;
            m.put("hitCount", hits);
            m.put("missCount", misses);
            m.put("hitRate", total > 0 ? Math.round(hits * 100.0 / total) : 0);
            m.put("loadCount", misses);  // L1 miss = load from L2/DB
            m.put("evictionCount", l1Cache.stats().evictionCount());
            m.put("estimatedSize", l1Cache.estimatedSize());
            result.put(name, m);
        }
        return result;
    }
}
