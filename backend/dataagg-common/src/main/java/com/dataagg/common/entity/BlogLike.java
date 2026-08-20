package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_like")
public class BlogLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long blogId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
