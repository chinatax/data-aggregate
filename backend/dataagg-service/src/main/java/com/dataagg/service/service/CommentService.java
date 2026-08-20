package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.Comment;
import com.dataagg.service.mapper.CommentMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    public List<Comment> listByBlogId(Long blogId) {
        return lambdaQuery()
                .eq(Comment::getBlogId, blogId)
                .orderByAsc(Comment::getCreateTime)
                .list();
    }

    public void addComment(Comment comment) {
        save(comment);
    }

    /**
     * 将扁平评论列表转为树形结构（顶级评论包含 children）
     */
    public List<Comment> buildCommentTree(List<Comment> allComments) {
        Map<Long, Comment> commentMap = allComments.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));
        List<Comment> roots = new ArrayList<>();
        for (Comment comment : allComments) {
            if (comment.getParentId() == null) {
                roots.add(comment);
            } else {
                Comment parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(comment);
                }
            }
        }
        return roots;
    }
}
