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
public class StockBoardItemVO {
    private String code;
    private String name;
    private BigDecimal changePct;
    private BigDecimal closePrice;
    private BigDecimal turnover;
    private String reason;
    private int boardCount;
}
