package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockBoardDaily;
import com.dataagg.service.mapper.StockBoardDailyMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockBoardService extends ServiceImpl<StockBoardDailyMapper, StockBoardDaily> {

    public List<StockBoardDaily> listByDate(LocalDate date) {
        LambdaQueryWrapper<StockBoardDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockBoardDaily::getTradeDate, date)
               .orderByDesc(StockBoardDaily::getBoardCount)
               .orderByDesc(StockBoardDaily::getChangePct);
        return this.list(wrapper);
    }

    public void deleteByDate(LocalDate date) {
        LambdaQueryWrapper<StockBoardDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockBoardDaily::getTradeDate, date);
        this.remove(wrapper);
    }

    public void deleteOlderThan(LocalDate threshold) {
        LambdaQueryWrapper<StockBoardDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockBoardDaily::getTradeDate, threshold);
        this.remove(wrapper);
    }
}
