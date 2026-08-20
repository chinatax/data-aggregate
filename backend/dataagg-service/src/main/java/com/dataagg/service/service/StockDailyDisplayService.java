package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockDailyDisplay;
import com.dataagg.service.mapper.StockDailyDisplayMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockDailyDisplayService extends ServiceImpl<StockDailyDisplayMapper, StockDailyDisplay> {

    @Transactional
    public void replaceAll(java.util.List<StockDailyDisplay> list) {
        // 清空旧数据，写入新数据（事务保护）
        this.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());
        if (!list.isEmpty()) {
            this.saveBatch(list);
        }
    }
}
