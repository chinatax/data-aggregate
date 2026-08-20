package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockFinancial;
import com.dataagg.service.mapper.StockFinancialMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StockFinancialService extends ServiceImpl<StockFinancialMapper, StockFinancial> {

    public List<StockFinancial> listByCode(String code) {
        LambdaQueryWrapper<StockFinancial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockFinancial::getCode, code)
               .orderByDesc(StockFinancial::getReportYear);
        return this.list(wrapper);
    }

    public void deleteAll() {
        this.baseMapper.delete(new LambdaQueryWrapper<>());
    }
}
