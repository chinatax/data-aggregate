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
public class MarginTradingVO {
    private BigDecimal rzye;
    private BigDecimal rqye;
    private BigDecimal rzrqye;
    private String tradeDate;
}
