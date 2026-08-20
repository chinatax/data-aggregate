package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("stock_board_count")
public class StockBoardCount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private Integer boardCount;
    private LocalDate updateDate;
}
