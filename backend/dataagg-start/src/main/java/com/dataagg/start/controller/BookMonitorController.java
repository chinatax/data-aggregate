package com.dataagg.start.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataagg.common.entity.BookMonitorLog;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.BookMonitorLogService;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.scheduler.BookMonitorScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/book-monitor")
public class BookMonitorController {

    @Autowired
    private BookMonitorLogService bookMonitorLogService;

    @Autowired
    private BookMonitorScheduler bookMonitorScheduler;

    @GetMapping("/latest")
    @RequireAuth("ADMIN")
    public Result<BookMonitorLog> latest() {
        BookMonitorLog latest = bookMonitorLogService.getLatest();
        return latest != null ? Result.success(latest) : Result.error("暂无监控记录");
    }

    @GetMapping("/history")
    @RequireAuth("ADMIN")
    public Result<Page<BookMonitorLog>> history(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BookMonitorLog> page = bookMonitorLogService.pageHistory(pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/trigger")
    @RequireAuth("ADMIN")
    public Result<String> triggerScan() {
        bookMonitorScheduler.scanBooks();
        return Result.success("扫描已触发");
    }
}
