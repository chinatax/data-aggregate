package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.BookCategory;
import com.dataagg.service.mapper.BookCategoryMapper;
import org.springframework.stereotype.Service;

@Service
public class BookCategoryService extends ServiceImpl<BookCategoryMapper, BookCategory> {

    public BookCategory getByName(String name) {
        return lambdaQuery().eq(BookCategory::getName, name).one();
    }
}
