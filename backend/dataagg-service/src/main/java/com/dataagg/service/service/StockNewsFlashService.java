package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockNewsFlash;
import com.dataagg.service.mapper.StockNewsFlashMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockNewsFlashService extends ServiceImpl<StockNewsFlashMapper, StockNewsFlash> {

    public List<StockNewsFlash> listLatest(int limit) {
        LambdaQueryWrapper<StockNewsFlash> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StockNewsFlash::getPublishTime)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public boolean existsByNewsId(String newsId) {
        return this.lambdaQuery()
                .eq(StockNewsFlash::getNewsId, newsId)
                .count() > 0;
    }

    public void deleteOlderThan(LocalDateTime threshold) {
        LambdaQueryWrapper<StockNewsFlash> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockNewsFlash::getCreateTime, threshold);
        this.remove(wrapper);
    }
}
