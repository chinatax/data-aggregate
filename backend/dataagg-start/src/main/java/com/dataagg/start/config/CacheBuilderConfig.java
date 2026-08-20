package com.dataagg.start.config;

import com.dataagg.start.cache.MultiLevelCache;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheBuilderConfig implements ApplicationRunner {

    private final MultiLevelCache cache;

    public CacheBuilderConfig(MultiLevelCache cache) {
        this.cache = cache;
    }

    @Override
    public void run(ApplicationArguments args) {
        // L1: short local TTL for frequently-changing data
        // L2: long Redis TTL for durability
        cache.register("categories", Duration.ofSeconds(30), Duration.ofMinutes(60));
        cache.register("blogPages", Duration.ofMinutes(2), Duration.ofMinutes(15));
        cache.register("blogs", Duration.ofMinutes(5), Duration.ofMinutes(10));
        cache.register("users", Duration.ofMinutes(1), Duration.ofMinutes(30));
        cache.register("siteConfig", Duration.ofMinutes(5), Duration.ofMinutes(120));
        cache.register("marketOverview", Duration.ofSeconds(60), Duration.ofSeconds(120));
    }
}
