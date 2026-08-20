package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_file_hash")
public class BlogFileHash {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("md5_hash")
    private String md5Hash;

    @TableField("blog_id")
    private Long blogId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
