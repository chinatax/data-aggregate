package com.dataagg.start.scheduler;

import com.dataagg.service.service.StockDailyAmountService;
import com.dataagg.service.service.StockDataService;
import com.dataagg.service.service.StockDragonBoardService;
import com.dataagg.service.service.StockGlobalNewsService;
import com.dataagg.service.service.StockHotTopicService;
import com.dataagg.service.service.StockIndustryRankService;
import com.dataagg.service.service.StockHolderNumService;
import com.dataagg.service.service.StockHolderTop10Service;
import com.dataagg.service.service.StockLockupExpiryService;
import com.dataagg.service.service.StockNewsFlashService;
import com.dataagg.start.service.SchedulerTaskTracker;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class StockDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockDataScheduler.class);

    @Autowired
    private SchedulerTaskTracker tracker;

    @Autowired
    private StockDataService stockDataService;

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
    private StockHolderNumService holderNumService;

    @Autowired
    private StockHolderTop10Service holderTop10Service;

    @Autowired
    private StockDailyAmountService dailyAmountService;

    @PostConstruct
    public void registerTasks() {
        tracker.register("pollIndices", "指数行情", "拉取三大指数实时行情", "行情数据",
                "FIXED_DELAY", "30s", () -> stockDataService.fetchAndCacheIndices());
        tracker.register("pollNewsFlashes", "快讯", "拉取实时快讯", "行情数据",
                "FIXED_DELAY", "60s", () -> stockDataService.fetchAndCacheNewsFlashes());
        tracker.register("pollHotTopics", "热门题材", "拉取热门题材板块", "行情数据",
                "FIXED_DELAY", "5min", () -> stockDataService.fetchAndCacheHotTopics());
        tracker.register("pollIndustryRank", "行业排行", "拉取行业板块涨跌排行", "行情数据",
                "FIXED_DELAY", "5min", () -> stockDataService.fetchAndCacheIndustryRank());
        tracker.register("pollNorthboundFlow", "北向资金", "拉取北向资金流向数据", "行情数据",
                "FIXED_DELAY", "60s", () -> stockDataService.fetchAndCacheNorthboundFlow());
        tracker.register("pollMarginTrading", "融资融券", "拉取融资融券余额数据", "行情数据",
                "FIXED_DELAY", "5min", () -> stockDataService.fetchAndCacheMarginTrading());
        tracker.register("pollDragonBoard", "龙虎榜", "拉取龙虎榜数据", "行情数据",
                "FIXED_DELAY", "2min", () -> stockDataService.fetchAndCacheDragonBoard());
        tracker.register("pollLockupExpiry", "解禁到期", "拉取限售解禁到期数据", "行情数据",
                "FIXED_DELAY", "1h", () -> stockDataService.fetchAndCacheLockupExpiry());
        tracker.register("pollBoardLadder", "连板天梯", "计算连板天梯排行", "行情数据",
                "FIXED_DELAY", "2min", () -> stockDataService.fetchAndCacheBoardLadder());
        tracker.register("pollGlobalNews", "全球新闻", "拉取全球财经新闻", "新闻数据",
                "FIXED_DELAY", "2min", () -> stockDataService.fetchAndCacheGlobalNews());
        tracker.register("pollHolderNum", "筹码集中度", "拉取股东人数变化数据", "行情数据",
                "FIXED_DELAY", "30min", () -> stockDataService.fetchAndCacheHolderNum());
        tracker.register("pollHolderTop10", "十大流通股东", "拉取十大流通股东数据", "行情数据",
                "FIXED_DELAY", "24h", () -> stockDataService.fetchAndCacheHolderTop10());
        tracker.register("cleanOldData", "数据清理", "清理3天前快讯/30天前题材/7天前排行等过期数据", "系统维护",
                "CRON", "3:05", () -> doCleanOldData());
        tracker.register("pollQualityScores", "优质评分", "计算优质公司评分", "行情数据",
                "CRON", "2:30", () -> stockDataService.fetchAndCacheQualityScores());
        tracker.register("pollDailyAmount", "日成交数据", "收盘后采集全A股成交额/换手率/量比/流通市值/行业/概念/地区", "行情数据",
                "CRON", "15:30(交易日)", () -> stockDataService.fetchAndCacheDailyAmounts());
        tracker.register("refreshDailyDisplay", "展示数据刷新", "0点将最新交易日数据写入展示缓存", "行情数据",
                "CRON", "00:00", () -> stockDataService.refreshDailyDisplay());
    }

    @Scheduled(initialDelayString = "${stock.index.initialDelay:5000}",
               fixedDelayString = "${stock.index.interval:30000}")
    public void pollIndices() {
        wrap("pollIndices", () -> stockDataService.fetchAndCacheIndices());
    }

    @Scheduled(initialDelayString = "${stock.newsflash.initialDelay:10000}",
               fixedDelayString = "${stock.newsflash.interval:60000}")
    public void pollNewsFlashes() {
        wrap("pollNewsFlashes", () -> stockDataService.fetchAndCacheNewsFlashes());
    }

    @Scheduled(initialDelayString = "${stock.hottopic.initialDelay:20000}",
               fixedDelayString = "${stock.hottopic.interval:300000}")
    public void pollHotTopics() {
        wrap("pollHotTopics", () -> stockDataService.fetchAndCacheHotTopics());
    }

    @Scheduled(initialDelayString = "${stock.industry.initialDelay:30000}",
               fixedDelayString = "${stock.industry.interval:300000}")
    public void pollIndustryRank() {
        wrap("pollIndustryRank", () -> stockDataService.fetchAndCacheIndustryRank());
    }

    @Scheduled(initialDelayString = "${stock.northbound.initialDelay:5000}",
               fixedDelayString = "${stock.northbound.interval:60000}")
    public void pollNorthboundFlow() {
        wrap("pollNorthboundFlow", () -> stockDataService.fetchAndCacheNorthboundFlow());
    }

    @Scheduled(initialDelayString = "${stock.margin.initialDelay:10000}",
               fixedDelayString = "${stock.margin.interval:300000}")
    public void pollMarginTrading() {
        wrap("pollMarginTrading", () -> stockDataService.fetchAndCacheMarginTrading());
    }

    @Scheduled(initialDelayString = "${stock.dragonboard.initialDelay:15000}",
               fixedDelayString = "${stock.dragonboard.interval:120000}")
    public void pollDragonBoard() {
        wrap("pollDragonBoard", () -> stockDataService.fetchAndCacheDragonBoard());
    }

    @Scheduled(initialDelayString = "${stock.lockup.initialDelay:20000}",
               fixedDelayString = "${stock.lockup.interval:3600000}")
    public void pollLockupExpiry() {
        wrap("pollLockupExpiry", () -> stockDataService.fetchAndCacheLockupExpiry());
    }

    @Scheduled(initialDelayString = "${stock.boardladder.initialDelay:35000}",
               fixedDelayString = "${stock.boardladder.interval:120000}")
    public void pollBoardLadder() {
        wrap("pollBoardLadder", () -> stockDataService.fetchAndCacheBoardLadder());
    }

    @Scheduled(initialDelayString = "${stock.globalnews.initialDelay:25000}",
               fixedDelayString = "${stock.globalnews.interval:120000}")
    public void pollGlobalNews() {
        wrap("pollGlobalNews", () -> stockDataService.fetchAndCacheGlobalNews());
    }

    @Scheduled(initialDelayString = "${stock.holdernum.initialDelay:40000}",
               fixedDelayString = "${stock.holdernum.interval:1800000}")
    public void pollHolderNum() {
        wrap("pollHolderNum", () -> stockDataService.fetchAndCacheHolderNum());
    }

    @Scheduled(initialDelayString = "${stock.holdertop10.initialDelay:60000}",
               fixedDelayString = "${stock.holdertop10.interval:86400000}")
    public void pollHolderTop10() {
        wrap("pollHolderTop10", () -> stockDataService.fetchAndCacheHolderTop10());
    }

    @Scheduled(cron = "0 5 3 * * *")
    public void cleanOldData() {
        wrap("cleanOldData", this::doCleanOldData);
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void pollQualityScores() {
        wrap("pollQualityScores", () -> stockDataService.fetchAndCacheQualityScores());
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void pollDailyAmounts() {
        wrap("pollDailyAmount", () -> stockDataService.fetchAndCacheDailyAmounts());
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshDailyDisplay() {
        wrap("refreshDailyDisplay", () -> stockDataService.refreshDailyDisplay());
    }

    private void doCleanOldData() {
        log.info("Stock data cleanup started");
        newsFlashService.deleteOlderThan(LocalDateTime.now().minusDays(3));
        hotTopicService.deleteOlderThan(LocalDate.now().minusDays(30));
        industryRankService.deleteOlderThan(LocalDateTime.now().minusDays(7));
        dragonBoardService.deleteOlderThan(LocalDate.now().minusDays(7));
        lockupExpiryService.deleteOlderThan(LocalDate.now());
        globalNewsService.deleteOlderThan(LocalDateTime.now().minusDays(3));
        holderNumService.deleteOlderThan(LocalDate.now().minusDays(365));
        holderTop10Service.deleteOlderThan(LocalDate.now().minusMonths(12)); // 保留4个季度
        dailyAmountService.deleteOlderThan(LocalDate.now().minusDays(365));
        log.info("Stock data cleanup completed");
    }

    private void wrap(String taskId, Runnable task) {
        tracker.markRunning(taskId);
        try {
            task.run();
            tracker.markSuccess(taskId);
        } catch (Exception e) {
            tracker.markFailed(taskId, e.getMessage());
            log.error("{} failed", taskId, e);
        }
    }
}
