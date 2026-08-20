package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockDailyAmount;
import com.dataagg.service.mapper.StockDailyAmountMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockDailyAmountService extends ServiceImpl<StockDailyAmountMapper, StockDailyAmount> {

    public List<StockDailyAmount> listByDate(LocalDate date) {
        LambdaQueryWrapper<StockDailyAmount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockDailyAmount::getTradeDate, date)
               .orderByDesc(StockDailyAmount::getAmount);
        return this.list(wrapper);
    }

    public List<StockDailyAmount> listByCode(String code, int limit) {
        LambdaQueryWrapper<StockDailyAmount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockDailyAmount::getCode, code)
               .orderByDesc(StockDailyAmount::getTradeDate)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public void deleteByTradeDate(LocalDate date) {
        LambdaQueryWrapper<StockDailyAmount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockDailyAmount::getTradeDate, date);
        this.remove(wrapper);
    }

    public void deleteOlderThan(LocalDate threshold) {
        LambdaQueryWrapper<StockDailyAmount> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockDailyAmount::getTradeDate, threshold);
        this.remove(wrapper);
    }
}
