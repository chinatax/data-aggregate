package com.dataagg.start.controller;

import com.dataagg.common.entity.Comment;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.CommentService;
import com.dataagg.start.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/blog/{blogId}/comments")
    public Result<List<Comment>> list(@PathVariable Long blogId) {
        List<Comment> all = commentService.listByBlogId(blogId);
        List<Comment> tree = commentService.buildCommentTree(all);
        return Result.success(tree);
    }

    @PostMapping("/blog/{blogId}/comments")
    public Result<Comment> addComment(@PathVariable Long blogId, @RequestBody Map<String, String> body) {
        String loginUser = getLoginUsername();
        String authorName;
        if (loginUser != null) {
            authorName = loginUser;
        } else {
            authorName = body.getOrDefault("authorName", "").trim();
            if (authorName.isEmpty()) {
                authorName = "匿名用户";
            }
        }

        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return Result.error("留言内容不能为空");
        }
        if (content.length() > 2000) {
            return Result.error("留言内容不能超过2000字");
        }

        Comment comment = new Comment();
        comment.setBlogId(blogId);
        comment.setAuthorName(authorName);
        comment.setContent(content);
        commentService.addComment(comment);
        return Result.success(comment);
    }

    @PostMapping("/comments/{commentId}/reply")
    public Result<Comment> addReply(@PathVariable Long commentId, @RequestBody Map<String, String> body) {
        Comment parent = commentService.getById(commentId);
        if (parent == null) {
            return Result.error("原评论不存在");
        }

        String loginUser = getLoginUsername();
        String authorName;
        if (loginUser != null) {
            authorName = loginUser;
        } else {
            authorName = body.getOrDefault("authorName", "").trim();
            if (authorName.isEmpty()) {
                authorName = "匿名用户";
            }
        }

        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return Result.error("回复内容不能为空");
        }
        if (content.length() > 2000) {
            return Result.error("回复内容不能超过2000字");
        }

        Comment reply = new Comment();
        reply.setBlogId(parent.getBlogId());
        reply.setParentId(commentId);
        reply.setAuthorName(authorName);
        reply.setContent(content);
        commentService.addComment(reply);
        return Result.success(reply);
    }

    /**
     * 尝试从 Authorization 头解析 JWT 获取用户名，失败返回 null
     */
    private String getLoginUsername() {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtil.getUsername(header.substring(7));
        } catch (JwtException e) {
            return null;
        }
    }
}
