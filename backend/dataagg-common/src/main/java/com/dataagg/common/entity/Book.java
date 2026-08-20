package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String title;

    private String author;

    private String downloadUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
