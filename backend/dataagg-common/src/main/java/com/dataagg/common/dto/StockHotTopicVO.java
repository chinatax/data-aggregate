package com.dataagg.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHotTopicVO {
    private String code;
    private String name;
    private String reason;
    private BigDecimal changePct;
    private List<String> stocks;
    private String topicDate;
}
