package com.dataagg.common.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;
    private Long blogId;
    private Long parentId;
    private String authorName;
    private String content;
    private LocalDateTime createTime;
    private List<CommentVO> children;
}
