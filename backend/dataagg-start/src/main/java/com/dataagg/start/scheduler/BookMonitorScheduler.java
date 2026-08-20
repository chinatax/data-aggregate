package com.dataagg.start.scheduler;

import com.dataagg.common.entity.BookFileHash;
import com.dataagg.common.entity.BookMonitorLog;
import com.dataagg.service.service.BookFileHashService;
import com.dataagg.service.service.BookMonitorLogService;
import com.dataagg.service.service.BookSyncService;
import com.dataagg.start.service.SchedulerTaskTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BookMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookMonitorScheduler.class);

    @Value("${book.data-path:book/}")
    private String bookDataPath;

    @Autowired
    private SchedulerTaskTracker tracker;

    @Autowired
    private BookSyncService bookSyncService;

    @Autowired
    private BookFileHashService bookFileHashService;

    @Autowired
    private BookMonitorLogService bookMonitorLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void registerTasks() {
        tracker.register("scanBooks", "电子书监控", "扫描book目录.md文件MD5变更(增/改/删)并同步", "内容管理",
                "CRON", "每小时整点", this::doScanBooks);
    }

    @Scheduled(cron = "${book.monitor.cron:0 0 * * * *}")
    public void scanBooks() {
        tracker.markRunning("scanBooks");
        try {
            doScanBooks();
            tracker.markSuccess("scanBooks");
        } catch (Exception e) {
            tracker.markFailed("scanBooks", e.getMessage());
            log.error("scanBooks failed", e);
        }
    }

    private void doScanBooks() {
        log.info("Book monitor scan started");
        BookMonitorLog logEntry = new BookMonitorLog();
        logEntry.setScanTime(LocalDateTime.now());
        logEntry.setStatus("SUCCESS");

        Path dir = Paths.get(bookDataPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.warn("Book data directory not found: {}", bookDataPath);
            logEntry.setStatus("FAILED");
            logEntry.setEndTime(LocalDateTime.now());
            logEntry.setErrorMessage("[\"目录不存在: " + bookDataPath + "\"]");
            bookMonitorLogService.save(logEntry);
            return;
        }

        try {
            // 1. 扫描磁盘文件并计算 MD5
            List<Path> mdFiles;
            try (var stream = Files.list(dir)) {
                mdFiles = stream.filter(p -> p.toString().endsWith(".md")).collect(Collectors.toList());
            }
            Map<String, String> diskHashes = new LinkedHashMap<>();
            for (Path f : mdFiles) {
                String content = Files.readString(f);
                diskHashes.put(f.getFileName().toString(), bookSyncService.computeMd5(content));
            }

            // 2. 获取数据库中已有的哈希记录
            List<BookFileHash> dbHashList = bookFileHashService.list();
            Map<String, String> dbMap = new HashMap<>();
            for (BookFileHash h : dbHashList) {
                dbMap.put(h.getFileName(), h.getMd5Hash());
            }

            Set<String> diskNames = diskHashes.keySet();
            Set<String> dbNames = dbMap.keySet();

            // 3. 新增文件
            Set<String> added = new LinkedHashSet<>(diskNames);
            added.removeAll(dbNames);

            // 4. 修改文件
            Set<String> modified = new LinkedHashSet<>();
            for (String name : diskNames) {
                if (dbMap.containsKey(name) && !diskHashes.get(name).equals(dbMap.get(name))) {
                    modified.add(name);
                }
            }

            // 5. 删除文件
            Set<String> deleted = new LinkedHashSet<>(dbNames);
            deleted.removeAll(diskNames);

            log.info("Monitor detected: +{} added, ~{} modified, -{} deleted", added.size(), modified.size(), deleted.size());

            int booksAdded = 0, booksUpdated = 0;
            int filesAddedCount = 0, filesModifiedCount = 0;
            List<String> errors = new ArrayList<>();

            // 6. 同步新增
            for (String name : added) {
                try {
                    Map<String, Object> r = bookSyncService.syncSingleFile(dir.resolve(name));
                    booksAdded += (int) r.getOrDefault("bookCount", 0);
                    filesAddedCount++;
                    bookFileHashService.upsertByFileName(name, diskHashes.get(name));
                } catch (Exception e) {
                    errors.add("新增 " + name + ": " + e.getMessage());
                    log.error("Sync added file failed: {}", name, e);
                }
            }

            // 7. 同步修改
            for (String name : modified) {
                try {
                    Map<String, Object> r = bookSyncService.syncSingleFile(dir.resolve(name));
                    booksUpdated += (int) r.getOrDefault("bookCount", 0);
                    filesModifiedCount++;
                    bookFileHashService.upsertByFileName(name, diskHashes.get(name));
                } catch (Exception e) {
                    errors.add("修改 " + name + ": " + e.getMessage());
                    log.error("Sync modified file failed: {}", name, e);
                }
            }

            // 8. 处理删除
            int filesDeletedCount = 0, booksRemoved = 0;
            for (String name : deleted) {
                try {
                    Map<String, Object> r = bookSyncService.deleteCategoryForFile(name);
                    booksRemoved += (int) r.getOrDefault("booksDeleted", 0);
                    filesDeletedCount++;
                    bookFileHashService.removeByFileName(name);
                } catch (Exception e) {
                    errors.add("删除 " + name + ": " + e.getMessage());
                    log.error("Delete file cleanup failed: {}", name, e);
                }
            }

            // 9. 记录日志
            logEntry.setEndTime(LocalDateTime.now());
            logEntry.setFilesAdded(filesAddedCount);
            logEntry.setFilesModified(filesModifiedCount);
            logEntry.setFilesDeleted(filesDeletedCount);
            logEntry.setBooksAdded(booksAdded);
            logEntry.setBooksUpdated(booksUpdated);
            logEntry.setBooksRemoved(booksRemoved);
            if (!errors.isEmpty()) {
                logEntry.setStatus((filesAddedCount + filesModifiedCount + filesDeletedCount) > 0 ? "PARTIAL" : "FAILED");
                logEntry.setErrorMessage(objectMapper.writeValueAsString(errors));
            }
            bookMonitorLogService.save(logEntry);

            log.info("Book monitor scan complete: +{}/~{} files, +{}~{} books, errors={}",
                    filesAddedCount, filesModifiedCount, booksAdded, booksUpdated, errors.size());

        } catch (Exception e) {
            log.error("Book monitor scan failed", e);
            logEntry.setEndTime(LocalDateTime.now());
            logEntry.setStatus("FAILED");
            try {
                logEntry.setErrorMessage(objectMapper.writeValueAsString(List.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            } catch (Exception ex) { /* ignore */ }
            bookMonitorLogService.save(logEntry);
        }
    }
}
