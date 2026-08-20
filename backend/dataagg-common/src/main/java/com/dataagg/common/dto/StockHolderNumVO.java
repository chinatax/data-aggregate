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
public class StockHolderNumVO {
    private String endDate;
    private String code;
    private String name;
    private String industry;
    private String region;
    private Long holderNum;
    private Long changeNum;
    private BigDecimal changeRatio;
    private BigDecimal avgShares;
}
