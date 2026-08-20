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
public class StockLockupExpiryVO {
    private String code;
    private String name;
    private String freeDate;
    private String lockupType;
    private BigDecimal shares;
    private BigDecimal ratio;
}
