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
public class StockDragonBoardVO {
    private String tradeDate;
    private String code;
    private String name;
    private String reason;
    private BigDecimal closePrice;
    private BigDecimal changePct;
    private BigDecimal netBuy;
    private BigDecimal buyAmt;
    private BigDecimal sellAmt;
    private BigDecimal turnoverPct;
}
