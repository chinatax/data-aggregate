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
public class NorthboundFlowVO {
    private BigDecimal hgt;
    private BigDecimal sgt;
    private BigDecimal total;
    private String updateTime;
    private String tradeDate;
}
