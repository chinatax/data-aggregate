package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockHotTopic;
import com.dataagg.service.mapper.StockHotTopicMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockHotTopicService extends ServiceImpl<StockHotTopicMapper, StockHotTopic> {

    public List<StockHotTopic> listByDate(LocalDate date) {
        LambdaQueryWrapper<StockHotTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockHotTopic::getTopicDate, date)
               .orderByDesc(StockHotTopic::getChangePct);
        return this.list(wrapper);
    }

    public void deleteByDate(LocalDate date) {
        LambdaQueryWrapper<StockHotTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockHotTopic::getTopicDate, date);
        this.remove(wrapper);
    }

    public void deleteOlderThan(LocalDate threshold) {
        LambdaQueryWrapper<StockHotTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockHotTopic::getTopicDate, threshold);
        this.remove(wrapper);
    }

    public List<StockHotTopic> listLatest() {
        LambdaQueryWrapper<StockHotTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StockHotTopic::getTopicDate)
               .orderByDesc(StockHotTopic::getChangePct);
        wrapper.last("LIMIT 20");
        return this.list(wrapper);
    }
}
