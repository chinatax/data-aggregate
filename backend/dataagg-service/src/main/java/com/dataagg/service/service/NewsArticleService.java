package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.NewsArticle;
import com.dataagg.service.mapper.NewsArticleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NewsArticleService extends ServiceImpl<NewsArticleMapper, NewsArticle> {

    public List<NewsArticle> listLatest(int limit) {
        LambdaQueryWrapper<NewsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(NewsArticle::getPubDate)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public boolean existsByLink(String link) {
        return this.lambdaQuery()
                .eq(NewsArticle::getLink, link)
                .count() > 0;
    }

    public void deleteOlderThan(LocalDateTime threshold) {
        LambdaQueryWrapper<NewsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(NewsArticle::getCreateTime, threshold);
        this.remove(wrapper);
    }

    public List<NewsArticle> listLatestExcludingTax(int limit) {
        LambdaQueryWrapper<NewsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(NewsArticle::getCategory, "tax")
               .orderByDesc(NewsArticle::getPubDate)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public List<NewsArticle> listByCategory(String category, int limit) {
        LambdaQueryWrapper<NewsArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NewsArticle::getCategory, category)
               .orderByDesc(NewsArticle::getPubDate)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }
}
