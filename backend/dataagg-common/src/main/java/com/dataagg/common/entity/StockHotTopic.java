package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_hot_topic")
public class StockHotTopic {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate topicDate;

    private String code;

    private String name;

    private String reason;

    private BigDecimal changePct;

    private BigDecimal turnover;

    private Long volume;

    private BigDecimal closePrice;

    private String market;

    private LocalDateTime createTime;
}
