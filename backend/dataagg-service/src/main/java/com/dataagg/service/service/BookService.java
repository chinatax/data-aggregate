package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.Book;
import com.dataagg.service.mapper.BookMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BookService extends ServiceImpl<BookMapper, Book> {

    public Page<Book> pageByCondition(Page<Book> page, Long categoryId, String keyword, List<Long> bookIds) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Book::getTitle, keyword).or().like(Book::getAuthor, keyword));
        }
        if (bookIds != null && !bookIds.isEmpty()) {
            wrapper.in(Book::getId, bookIds);
        }
        wrapper.orderByAsc(Book::getTitle);
        return page(page, wrapper);
    }

    public void deleteByCategoryId(Long categoryId) {
        lambdaUpdate().eq(Book::getCategoryId, categoryId).remove();
    }

    public Map<Long, Long> countByCategory() {
        return list().stream()
                .collect(java.util.stream.Collectors.groupingBy(Book::getCategoryId, java.util.stream.Collectors.counting()));
    }
}
