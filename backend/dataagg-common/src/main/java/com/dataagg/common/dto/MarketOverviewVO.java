package com.dataagg.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOverviewVO {
    private List<StockIndexVO> indices;
    private List<StockNewsFlashVO> newsFlashes;
    private List<StockHotTopicVO> hotTopics;
    private Map<String, Object> stats;
}
