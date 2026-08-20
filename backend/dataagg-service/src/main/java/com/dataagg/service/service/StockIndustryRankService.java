package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockIndustryRank;
import com.dataagg.service.mapper.StockIndustryRankMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockIndustryRankService extends ServiceImpl<StockIndustryRankMapper, StockIndustryRank> {

    public List<StockIndustryRank> listLatest() {
        LambdaQueryWrapper<StockIndustryRank> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StockIndustryRank::getSnapTime)
               .last("LIMIT 100");
        return this.list(wrapper);
    }

    public void deleteOlderThan(LocalDateTime threshold) {
        LambdaQueryWrapper<StockIndustryRank> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(StockIndustryRank::getCreateTime, threshold);
        this.remove(wrapper);
    }
}
