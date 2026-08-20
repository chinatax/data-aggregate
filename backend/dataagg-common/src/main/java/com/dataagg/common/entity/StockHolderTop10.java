package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_holder_top10")
public class StockHolderTop10 {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate endDate;
    private String code;
    private String name;
    private String holderName;
    private Long holdNum;
    private BigDecimal holdRatio;
    private String holdChange;
    private Integer holderRank;
    private String sharesType;
    private LocalDateTime createTime;
}
