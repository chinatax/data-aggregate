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
public class StockDailyAmountVO {
    private String tradeDate;
    private String code;
    private String name;
    private BigDecimal amount;
    private BigDecimal turnoverPct;
    private BigDecimal volumeRatio;
    private BigDecimal floatMarketCap;
    private String industry;
    private String concept;
    private String region;
    private BigDecimal prevAmount;
    private Integer boardCount;
    private Integer top100Cs;
}
