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
public class StockHolderTop10VO {
    private String endDate;
    private String code;
    private String name;
    private String holderName;
    private Long holdNum;
    private BigDecimal holdRatio;
    private String holdChange;
    private Integer holderRank;
    private String sharesType;
}
