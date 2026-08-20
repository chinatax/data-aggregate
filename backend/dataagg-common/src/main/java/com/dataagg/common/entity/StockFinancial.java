package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_financial")
public class StockFinancial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private Integer reportYear;
    private BigDecimal mainRevenue;
    private BigDecimal deductedNetProfit;
    private BigDecimal netProfit;
    private BigDecimal operatingCashFlow;
    private LocalDateTime createTime;
}
