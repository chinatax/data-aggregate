package com.dataagg.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardLadderVO {
    private String tradeDate;
    private List<StockBoardItemVO> firstBoard;
    private List<StockBoardItemVO> secondBoard;
    private List<StockBoardItemVO> thirdPlusBoard;
    private int totalCount;
}
