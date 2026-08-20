package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book_category")
public class BookCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
