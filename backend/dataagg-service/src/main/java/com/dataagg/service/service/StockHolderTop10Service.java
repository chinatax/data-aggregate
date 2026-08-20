package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockHolderTop10;
import com.dataagg.service.mapper.StockHolderTop10Mapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockHolderTop10Service extends ServiceImpl<StockHolderTop10Mapper, StockHolderTop10> {

    public List<StockHolderTop10> listByEndDate(LocalDate endDate) {
        LambdaQueryWrapper<StockHolderTop10> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockHolderTop10::getEndDate, endDate)
               .orderByAsc(StockHolderTop10::getCode)
               .orderByAsc(StockHolderTop10::getHolderRank);
        return this.list(wrapper);
    }

    public void deleteByEndDate(LocalDate date) {
        LambdaQueryWrapper<StockHolderTop10> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockHolderTop10::getEndDate, date);
        this.remove(wrapper);
    }

    public List<StockHolderTop10> listLatest() {
        LambdaQueryWrapper<StockHolderTop10> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StockHolderTop10::getEndDate)
               .orderByAsc(StockHolderTop10::getCode)
               .orderByAsc(StockHolderTop10::getHolderRank);
        return this.list(wrapper);
    }

    public void deleteAll() {
        LambdaQueryWrapper<StockHolderTop10> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("1=1");
        this.remove(wrapper);
    }

    public void deleteOlderThan(LocalDate threshold) {
        LambdaQueryWrapper<StockHolderTop10> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockHolderTop10::getEndDate, threshold);
        this.remove(wrapper);
    }
}
