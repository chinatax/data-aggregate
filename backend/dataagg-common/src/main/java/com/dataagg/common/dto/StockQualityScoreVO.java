package com.dataagg.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQualityScoreVO {
    private String code;
    private String name;
    private String industry;
    private BigDecimal score;
    private Integer revenuePass;
    private BigDecimal cashRevenueRatio;
    private Integer profitPass;
    private BigDecimal cashProfitRatio;
    private String detailJson;
}
