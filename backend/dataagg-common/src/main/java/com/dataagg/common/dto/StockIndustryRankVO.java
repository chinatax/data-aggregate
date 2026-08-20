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
public class StockIndustryRankVO {
    private String code;
    private String name;
    private BigDecimal changePct;
    private Integer upCount;
    private Integer downCount;
    private String leaderName;
    private BigDecimal leaderChange;
}
