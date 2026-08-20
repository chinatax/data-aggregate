package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book_monitor_log")
public class BookMonitorLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime scanTime;

    private LocalDateTime endTime;

    private Integer filesAdded;

    private Integer filesModified;

    private Integer filesDeleted;

    private Integer booksAdded;

    private Integer booksUpdated;

    private Integer booksRemoved;

    @TableField("error_message")
    private String errorMessage;

    private String status;

    private String details;
}
