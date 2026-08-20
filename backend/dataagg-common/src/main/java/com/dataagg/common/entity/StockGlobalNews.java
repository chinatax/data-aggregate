package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_global_news")
public class StockGlobalNews {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String newsCode;
    private String title;
    private String summary;
    private String showTime;
    private LocalDateTime createTime;
}
