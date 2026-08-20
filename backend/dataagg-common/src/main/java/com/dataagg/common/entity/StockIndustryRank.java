package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_industry_rank")
public class StockIndustryRank {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime snapTime;

    private String code;

    private String name;

    private BigDecimal changePct;

    private Integer upCount;

    private Integer downCount;

    private String leaderName;

    private BigDecimal leaderChange;

    private LocalDateTime createTime;
}
