package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockDragonBoard;
import com.dataagg.service.mapper.StockDragonBoardMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockDragonBoardService extends ServiceImpl<StockDragonBoardMapper, StockDragonBoard> {

    public List<StockDragonBoard> listByTradeDate(LocalDate date, int limit) {
        LambdaQueryWrapper<StockDragonBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockDragonBoard::getTradeDate, date)
               .orderByDesc(StockDragonBoard::getNetBuy)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public List<StockDragonBoard> listLatest(int limit) {
        LambdaQueryWrapper<StockDragonBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StockDragonBoard::getTradeDate)
               .orderByDesc(StockDragonBoard::getNetBuy)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public void deleteByTradeDate(LocalDate date) {
        LambdaQueryWrapper<StockDragonBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockDragonBoard::getTradeDate, date);
        this.remove(wrapper);
    }

    public void deleteOlderThan(LocalDate threshold) {
        LambdaQueryWrapper<StockDragonBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockDragonBoard::getTradeDate, threshold);
        this.remove(wrapper);
    }
}
