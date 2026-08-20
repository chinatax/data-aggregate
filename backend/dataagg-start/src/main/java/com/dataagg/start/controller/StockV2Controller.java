package com.dataagg.start.controller;

import com.dataagg.common.dto.BoardLadderVO;
import com.dataagg.common.dto.MarketOverviewVO;
import com.dataagg.common.dto.MarginTradingHistoryVO;
import com.dataagg.common.dto.MarginTradingVO;
import com.dataagg.common.dto.NorthboundFlowVO;
import com.dataagg.common.dto.StockDragonBoardVO;
import com.dataagg.common.dto.StockGlobalNewsVO;
import com.dataagg.common.dto.StockHotTopicVO;
import com.dataagg.common.dto.StockIndexVO;
import com.dataagg.common.dto.StockHolderNumVO;
import com.dataagg.common.dto.StockHolderTop10VO;
import com.dataagg.common.dto.StockKlineVO;
import com.dataagg.common.dto.StockLockupExpiryVO;
import com.dataagg.common.dto.StockDailyAmountVO;
import com.dataagg.common.dto.StockNewsFlashVO;
import com.dataagg.common.dto.StockQualityScoreVO;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.StockDataService;
import com.dataagg.start.cache.MultiLevelCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock/v2")
public class StockV2Controller {

    @Autowired
    private StockDataService stockDataService;

    @Autowired
    private MultiLevelCache cache;

    // 市场概览只展示主要指数
    private static final java.util.Set<String> MAIN_INDEX_CODES = java.util.Set.of(
            "sh000001", "sh000016", "sh000300", "sh000905",
            "sz399001", "sz399006", "sz399005", "sz399330");

    @GetMapping("/market")
    public Result<MarketOverviewVO> getMarketOverview() {
        MarketOverviewVO vo = cache.get("marketOverview", "current", MarketOverviewVO.class, () -> {
            List<StockIndexVO> allIndices = stockDataService.getIndicesFromCache();
            List<StockIndexVO> indices = allIndices.stream()
                    .filter(i -> MAIN_INDEX_CODES.contains(i.getCode()))
                    .collect(java.util.stream.Collectors.toList());
            List<StockNewsFlashVO> allNews = stockDataService.getNewsFlashesFromCache();
            List<StockNewsFlashVO> newsFlashes = allNews.size() > 8 ? allNews.subList(0, 8) : allNews;
            List<StockHotTopicVO> hotTopics = stockDataService.getHotTopicsFromCache();
            Map<String, Object> stats = stockDataService.getStats();
            return MarketOverviewVO.builder()
                    .indices(indices)
                    .newsFlashes(newsFlashes)
                    .hotTopics(hotTopics)
                    .stats(stats)
                    .build();
        });
        return Result.success(vo);
    }

    @GetMapping("/indices")
    public Result<List<StockIndexVO>> getIndices() {
        return Result.success(stockDataService.getIndicesFromCache());
    }

    @GetMapping("/newsflash")
    public Result<List<StockNewsFlashVO>> getNewsFlash() {
        return Result.success(stockDataService.getNewsFlashesFromCache());
    }

    @GetMapping("/hottopics")
    public Result<List<StockHotTopicVO>> getHotTopics() {
        return Result.success(stockDataService.getHotTopicsFromCache());
    }

    @GetMapping("/northbound")
    public Result<NorthboundFlowVO> getNorthboundFlow() {
        return Result.success(stockDataService.getNorthboundFlowFromCache());
    }

    @GetMapping("/margin")
    public Result<MarginTradingVO> getMarginTrading() {
        return Result.success(stockDataService.getMarginTradingFromCache());
    }

    @GetMapping("/margin/history")
    public Result<List<MarginTradingHistoryVO>> getMarginHistory() {
        return Result.success(stockDataService.getMarginHistoryFromCache());
    }

    @GetMapping("/dragonboard")
    public Result<List<StockDragonBoardVO>> getDragonBoard() {
        return Result.success(stockDataService.getDragonBoardFromCache());
    }

    @GetMapping("/lockup")
    public Result<List<StockLockupExpiryVO>> getLockupExpiry() {
        return Result.success(stockDataService.getLockupExpiryFromCache());
    }

    @GetMapping("/globalnews")
    public Result<List<StockGlobalNewsVO>> getGlobalNews() {
        return Result.success(stockDataService.getGlobalNewsFromCache());
    }

    @GetMapping("/board-ladder")
    public Result<BoardLadderVO> getBoardLadder() {
        return Result.success(stockDataService.getBoardLadderFromCache());
    }

    @GetMapping("/holder-num")
    public Result<List<StockHolderNumVO>> getHolderNum() {
        return Result.success(stockDataService.getHolderNumFromCache());
    }

    @GetMapping("/holder-top10")
    public Result<List<StockHolderTop10VO>> getHolderTop10() {
        return Result.success(stockDataService.getHolderTop10FromCache());
    }

    @GetMapping("/quality")
    public Result<List<StockQualityScoreVO>> getQualityScores() {
        return Result.success(stockDataService.getQualityScoresFromCache());
    }

    @GetMapping("/kline")
    public Result<List<StockKlineVO>> getKline(@RequestParam String code,
                                                @RequestParam(defaultValue = "30") int days) {
        return Result.success(stockDataService.getKline(code, days));
    }

    @GetMapping("/names")
    public Result<List<Map<String, Object>>> getStockNames() {
        return Result.success(stockDataService.getStockNames());
    }

    @GetMapping("/quality/refresh")
    public Result<List<StockQualityScoreVO>> refreshQualityScores() {
        return Result.success(stockDataService.fetchAndCacheQualityScores());
    }

    @GetMapping("/daily-amount/display")
    public Result<List<StockDailyAmountVO>> getDailyDisplay() {
        return Result.success(stockDataService.getDailyDisplay());
    }

    @GetMapping("/daily-amount/latest")
    public Result<List<StockDailyAmountVO>> getLatestDailyAmounts() {
        return Result.success(stockDataService.getLatestDailyAmounts());
    }

    @GetMapping("/daily-amount")
    public Result<List<StockDailyAmountVO>> getDailyAmount(@RequestParam String code,
                                                            @RequestParam(defaultValue = "30") int days) {
        return Result.success(stockDataService.getDailyAmountList(code, days));
    }

    @GetMapping("/daily-amount/refresh")
    public Result<List<StockDailyAmountVO>> refreshDailyAmounts() {
        return Result.success(stockDataService.fetchAndCacheDailyAmounts());
    }


}
