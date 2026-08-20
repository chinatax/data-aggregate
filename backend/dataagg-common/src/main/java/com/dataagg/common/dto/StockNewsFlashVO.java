package com.dataagg.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockNewsFlashVO {
    private String id;
    private String title;
    private String content;
    private Long publishTime;
}
