package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.BlogLike;
import com.dataagg.service.mapper.BlogLikeMapper;
import org.springframework.stereotype.Service;

@Service
public class BlogLikeService extends ServiceImpl<BlogLikeMapper, BlogLike> {

    public boolean isLiked(Long userId, Long blogId) {
        return lambdaQuery()
                .eq(BlogLike::getUserId, userId)
                .eq(BlogLike::getBlogId, blogId)
                .count() > 0;
    }

    public boolean toggle(Long userId, Long blogId) {
        BlogLike existing = lambdaQuery()
                .eq(BlogLike::getUserId, userId)
                .eq(BlogLike::getBlogId, blogId)
                .one();
        if (existing != null) {
            removeById(existing.getId());
            return false;
        }
        BlogLike l = new BlogLike();
        l.setUserId(userId);
        l.setBlogId(blogId);
        save(l);
        return true;
    }

    public long countByBlogId(Long blogId) {
        return lambdaQuery().eq(BlogLike::getBlogId, blogId).count();
    }
}
