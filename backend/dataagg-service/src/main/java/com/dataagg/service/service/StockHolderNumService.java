package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockHolderNum;
import com.dataagg.service.mapper.StockHolderNumMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockHolderNumService extends ServiceImpl<StockHolderNumMapper, StockHolderNum> {

    public List<StockHolderNum> listLatest(int limit) {
        LambdaQueryWrapper<StockHolderNum> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(StockHolderNum::getChangeRatio)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public void deleteByEndDate(LocalDate date) {
        LambdaQueryWrapper<StockHolderNum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockHolderNum::getEndDate, date);
        this.remove(wrapper);
    }

    public void deleteOlderThan(LocalDate threshold) {
        LambdaQueryWrapper<StockHolderNum> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockHolderNum::getEndDate, threshold);
        this.remove(wrapper);
    }
}
