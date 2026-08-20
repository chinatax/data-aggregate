package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_quality_score")
public class StockQualityScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String industry;
    private BigDecimal score;
    private Integer revenuePass;
    private BigDecimal cashRevenueRatio;
    private Integer profitPass;
    private BigDecimal cashProfitRatio;
    private String detailJson;
    private LocalDate tradeDate;
    private LocalDateTime createTime;
}
