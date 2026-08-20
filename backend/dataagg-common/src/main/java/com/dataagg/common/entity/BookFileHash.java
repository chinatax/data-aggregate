package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book_file_hash")
public class BookFileHash {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String md5Hash;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
