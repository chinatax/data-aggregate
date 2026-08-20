package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockBoardCount;
import com.dataagg.service.mapper.StockBoardCountMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockBoardCountService extends ServiceImpl<StockBoardCountMapper, StockBoardCount> {

    /** 读取所有上榜次数为 Map<code, count> */
    public Map<String, Integer> loadAsMap() {
        List<StockBoardCount> list = this.list();
        return list.stream().collect(Collectors.toMap(
                StockBoardCount::getCode,
                s -> s.getBoardCount() != null ? s.getBoardCount() : 0));
    }
}
