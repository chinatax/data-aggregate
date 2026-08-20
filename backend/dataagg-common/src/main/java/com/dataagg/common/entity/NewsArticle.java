package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news_article")
public class NewsArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String source;

    private String description;

    private String link;

    private LocalDateTime pubDate;

    private String imageUrl;

    private String country;

    private String category;

    private LocalDateTime createTime;
}
