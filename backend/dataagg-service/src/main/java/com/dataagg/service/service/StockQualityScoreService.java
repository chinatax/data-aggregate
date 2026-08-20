package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockQualityScore;
import com.dataagg.service.mapper.StockQualityScoreMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StockQualityScoreService extends ServiceImpl<StockQualityScoreMapper, StockQualityScore> {

    public List<StockQualityScore> listLatest() {
        return this.baseMapper.listLatest();
    }

    public List<StockQualityScore> listByIndustry(String industry) {
        LambdaQueryWrapper<StockQualityScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockQualityScore::getIndustry, industry)
               .orderByDesc(StockQualityScore::getScore);
        return this.list(wrapper);
    }

    public List<StockQualityScore> listTop(int limit) {
        List<StockQualityScore> list = this.baseMapper.listLatest();
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    public void deleteAll() {
        this.baseMapper.delete(new LambdaQueryWrapper<>());
    }
}
