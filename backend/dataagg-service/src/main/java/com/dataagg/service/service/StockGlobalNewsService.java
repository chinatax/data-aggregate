package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockGlobalNews;
import com.dataagg.service.mapper.StockGlobalNewsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockGlobalNewsService extends ServiceImpl<StockGlobalNewsMapper, StockGlobalNews> {

    public List<StockGlobalNews> listLatest(int limit) {
        LambdaQueryWrapper<StockGlobalNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StockGlobalNews::getCreateTime)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public boolean existsByNewsCode(String newsCode) {
        return this.lambdaQuery()
                .eq(StockGlobalNews::getNewsCode, newsCode)
                .count() > 0;
    }

    public void deleteOlderThan(LocalDateTime threshold) {
        LambdaQueryWrapper<StockGlobalNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockGlobalNews::getCreateTime, threshold);
        this.remove(wrapper);
    }
}
