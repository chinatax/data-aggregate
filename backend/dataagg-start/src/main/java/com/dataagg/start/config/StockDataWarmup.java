package com.dataagg.start.config;

import com.dataagg.service.service.StockDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CompletableFuture;

@Configuration
public class StockDataWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockDataWarmup.class);

    private final StockDataService stockDataService;

    public StockDataWarmup(StockDataService stockDataService) {
        this.stockDataService = stockDataService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Stock data cache warmup started");
        CompletableFuture.runAsync(() -> {
            try { stockDataService.fetchAndCacheIndices(); } catch (Exception e) { log.warn("warmup indices: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheNewsFlashes(); } catch (Exception e) { log.warn("warmup newsFlash: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheHotTopics(); } catch (Exception e) { log.warn("warmup hotTopics: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheIndustryRank(); } catch (Exception e) { log.warn("warmup industry: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheNorthboundFlow(); } catch (Exception e) { log.warn("warmup northbound: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheMarginTrading(); } catch (Exception e) { log.warn("warmup margin: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheMarginHistory(); } catch (Exception e) { log.warn("warmup marginHistory: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheDragonBoard(); } catch (Exception e) { log.warn("warmup dragonBoard: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheLockupExpiry(); } catch (Exception e) { log.warn("warmup lockup: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheGlobalNews(); } catch (Exception e) { log.warn("warmup globalNews: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheBoardLadder(); } catch (Exception e) { log.warn("warmup boardLadder: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheHolderNum(); } catch (Exception e) { log.warn("warmup holderNum: {}", e.getMessage()); }
            try { stockDataService.fetchAndCacheHolderTop10(); } catch (Exception e) { log.warn("warmup holderTop10: {}", e.getMessage()); }
            try { stockDataService.getStockNames(); } catch (Exception e) { log.warn("warmup stockNames: {}", e.getMessage()); }
            log.info("Stock data cache warmup completed");
        });
    }
}
