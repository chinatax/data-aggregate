package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockLockupExpiry;
import com.dataagg.service.mapper.StockLockupExpiryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockLockupExpiryService extends ServiceImpl<StockLockupExpiryMapper, StockLockupExpiry> {

    public List<StockLockupExpiry> listUpcoming(int limit) {
        LambdaQueryWrapper<StockLockupExpiry> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(StockLockupExpiry::getFreeDate, LocalDate.now())
               .orderByAsc(StockLockupExpiry::getFreeDate)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    public void deleteAllFuture() {
        LambdaQueryWrapper<StockLockupExpiry> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(StockLockupExpiry::getFreeDate, LocalDate.now());
        this.remove(wrapper);
    }

    public void deleteOlderThan(LocalDate threshold) {
        LambdaQueryWrapper<StockLockupExpiry> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockLockupExpiry::getFreeDate, threshold);
        this.remove(wrapper);
    }
}
