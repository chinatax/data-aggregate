package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.BookMonitorLog;
import com.dataagg.service.mapper.BookMonitorLogMapper;
import org.springframework.stereotype.Service;

@Service
public class BookMonitorLogService extends ServiceImpl<BookMonitorLogMapper, BookMonitorLog> {

    public BookMonitorLog getLatest() {
        return lambdaQuery()
                .orderByDesc(BookMonitorLog::getScanTime)
                .last("LIMIT 1")
                .one();
    }

    public Page<BookMonitorLog> pageHistory(Integer pageNum, Integer pageSize) {
        Page<BookMonitorLog> page = new Page<>(pageNum, pageSize);
        return lambdaQuery()
                .orderByDesc(BookMonitorLog::getScanTime)
                .page(page);
    }
}
