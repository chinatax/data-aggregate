package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_board_daily")
public class StockBoardDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate tradeDate;

    private String code;

    private String name;

    private BigDecimal changePct;

    private BigDecimal closePrice;

    private BigDecimal turnover;

    private String reason;

    private Integer boardCount;

    private LocalDateTime createTime;
}
