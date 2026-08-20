package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_holder_num")
public class StockHolderNum {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate endDate;
    private String code;
    private String name;
    private String industry;
    private String region;
    private Long holderNum;
    private Long changeNum;
    private BigDecimal changeRatio;
    private BigDecimal avgShares;
    private LocalDateTime createTime;
}
