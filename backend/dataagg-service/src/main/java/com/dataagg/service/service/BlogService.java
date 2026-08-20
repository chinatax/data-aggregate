package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.Blog;
import com.dataagg.service.mapper.BlogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlogService extends ServiceImpl<BlogMapper, Blog> {

    public Page<Blog> pageByCondition(Page<Blog> page, String keyword, String category, String status, String currentUser) {
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
        // 列表查询排除 content 大字段，减少 DB→应用→JSON 传输量
        wrapper.select(Blog.class, info -> !"content".equals(info.getColumn()));
        wrapper.eq(Blog::getDeleted, 0);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Blog::getStatus, status);
        } else {
            // 公开文章 + 当前用户的私密文章（草稿不显示在列表中）
            if (currentUser != null && !currentUser.isEmpty()) {
                wrapper.and(w -> w.eq(Blog::getStatus, "PUBLISHED")
                        .or(i -> i.eq(Blog::getStatus, "PRIVATE")
                                 .eq(Blog::getAuthor, currentUser)));
            } else {
                wrapper.eq(Blog::getStatus, "PUBLISHED");
            }
        }

        if (category != null && !category.isEmpty() && !"ALL".equals(category)) {
            wrapper.eq(Blog::getCategory, category);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Blog::getTitle, keyword)
                    .or()
                    .like(Blog::getSummary, keyword)
                    .or()
                    .like(Blog::getTags, keyword));
        }

        wrapper.orderByDesc(Blog::getCreateTime);
        return this.page(page, wrapper);
    }

    public Blog getByIdWithViewCount(Long id) {
        Blog blog = this.getById(id);
        if (blog != null) {
            this.lambdaUpdate()
                    .setSql("view_count = view_count + 1")
                    .eq(Blog::getId, id)
                    .update();
            blog.setViewCount(blog.getViewCount() + 1);
        }
        return blog;
    }

    public void incrementViewCount(Long id) {
        this.lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(Blog::getId, id)
                .update();
    }

    public List<Blog> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return this.lambdaQuery()
                .in(Blog::getId, ids)
                .orderByDesc(Blog::getCreateTime)
                .list();
    }
}
