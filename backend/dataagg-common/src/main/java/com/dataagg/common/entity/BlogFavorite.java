package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_favorite")
public class BlogFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long blogId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
