package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.BlogFavorite;
import com.dataagg.service.mapper.BlogFavoriteMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlogFavoriteService extends ServiceImpl<BlogFavoriteMapper, BlogFavorite> {

    public boolean isFavorited(Long userId, Long blogId) {
        return lambdaQuery()
                .eq(BlogFavorite::getUserId, userId)
                .eq(BlogFavorite::getBlogId, blogId)
                .count() > 0;
    }

    public boolean toggle(Long userId, Long blogId) {
        BlogFavorite existing = lambdaQuery()
                .eq(BlogFavorite::getUserId, userId)
                .eq(BlogFavorite::getBlogId, blogId)
                .one();
        if (existing != null) {
            removeById(existing.getId());
            return false;
        }
        BlogFavorite f = new BlogFavorite();
        f.setUserId(userId);
        f.setBlogId(blogId);
        save(f);
        return true;
    }

    public List<Long> getFavoritedBlogIds(Long userId) {
        return lambdaQuery()
                .select(BlogFavorite::getBlogId)
                .eq(BlogFavorite::getUserId, userId)
                .list()
                .stream()
                .map(BlogFavorite::getBlogId)
                .collect(Collectors.toList());
    }

    public Map<Long, Long> countByBlogIds(List<Long> blogIds) {
        if (blogIds == null || blogIds.isEmpty()) return Map.of();
        List<Map<String, Object>> rows = getBaseMapper().countGroupByBlogIds(blogIds);
        return rows.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("blog_id")).longValue(),
                        r -> ((Number) r.get("cnt")).longValue()));
    }

    public long countByBlogId(Long blogId) {
        return lambdaQuery().eq(BlogFavorite::getBlogId, blogId).count();
    }
}
