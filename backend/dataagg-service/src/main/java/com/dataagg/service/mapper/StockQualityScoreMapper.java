package com.dataagg.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataagg.common.entity.StockQualityScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface StockQualityScoreMapper extends BaseMapper<StockQualityScore> {

    @Select("SELECT * FROM stock_quality_score WHERE trade_date = (SELECT MAX(trade_date) FROM stock_quality_score) ORDER BY score DESC")
    List<StockQualityScore> listLatest();
}
