package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_daily_display")
public class StockDailyDisplay {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate tradeDate;
    private String code;
    private String name;
    private BigDecimal amount;
    private BigDecimal turnoverPct;
    private BigDecimal volumeRatio;
    private BigDecimal floatMarketCap;
    private String industry;
    private String concept;
    private String region;
    private Integer boardCount;
    private LocalDateTime createTime;
}
