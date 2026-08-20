package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.Category;
import com.dataagg.service.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

@Service
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    public Category getByCode(String code) {
        return lambdaQuery().eq(Category::getCode, code).one();
    }
}
