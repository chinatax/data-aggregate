package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog")
public class Blog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    private String coverImage;

    private String tags;

    private String category;

    private String author;

    private Integer viewCount;

    private Integer likeCount;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(exist = false)
    private String authorAvatar;

    @TableField(exist = false)
    private Boolean favorited;

    @TableField(exist = false)
    private Boolean liked;

    @TableField(exist = false)
    private Long favoriteCount;
}
