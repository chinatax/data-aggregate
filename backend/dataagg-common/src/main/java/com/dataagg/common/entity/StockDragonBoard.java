package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_dragon_board")
public class StockDragonBoard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate tradeDate;
    private String code;
    private String name;
    private String reason;
    private BigDecimal closePrice;
    private BigDecimal changePct;
    private BigDecimal netBuy;
    private BigDecimal buyAmt;
    private BigDecimal sellAmt;
    private BigDecimal turnoverPct;
    private LocalDateTime createTime;
}
