package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_lockup_expiry")
public class StockLockupExpiry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private LocalDate freeDate;
    private String lockupType;
    private BigDecimal shares;
    private BigDecimal ratio;
    private LocalDateTime createTime;
}
