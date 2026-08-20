package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_news_flash")
public class StockNewsFlash {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String newsId;

    private String title;

    private String content;

    private Long publishTime;

    private LocalDateTime createTime;
}
