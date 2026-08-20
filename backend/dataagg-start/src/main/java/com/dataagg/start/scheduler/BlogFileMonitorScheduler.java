package com.dataagg.start.scheduler;

import com.dataagg.common.entity.Category;
import com.dataagg.service.service.BlogSyncService;
import com.dataagg.service.service.CategoryService;
import com.dataagg.start.service.SchedulerTaskTracker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogFileMonitorScheduler {

    private final BlogSyncService blogSyncService;
    private final CategoryService categoryService;
    private final SchedulerTaskTracker tracker;

    @Value("${blog.data-path:/app/blog}")
    private String blogDataPath;

    @PostConstruct
    public void registerTasks() {
        tracker.register("scanAndSync", "博客同步", "扫描blog目录.md文件变更并同步到数据库", "内容管理",
                "FIXED_DELAY", "60s", this::doScanAndSync);
    }

    @Scheduled(initialDelayString = "${blog.monitor.initialDelay:30000}",
               fixedDelayString = "${blog.monitor.interval:60000}")
    public void scanAndSync() {
        tracker.markRunning("scanAndSync");
        try {
            doScanAndSync();
            tracker.markSuccess("scanAndSync");
        } catch (Exception e) {
            tracker.markFailed("scanAndSync", e.getMessage());
            log.error("scanAndSync failed", e);
        }
    }

    private void doScanAndSync() {
        Path rootDir = Paths.get(blogDataPath);
        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            log.debug("Blog data directory does not exist: {}", blogDataPath);
            return;
        }

        List<Path> mdFiles;
        try (Stream<Path> stream = Files.walk(rootDir)) {
            mdFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .toList();
        } catch (IOException e) {
            log.error("Failed to scan blog directory: {}", blogDataPath, e);
            return;
        }

        int synced = 0;
        int skipped = 0;
        for (Path mdFile : mdFiles) {
            try {
                Path relativePath = rootDir.relativize(mdFile);
                String category = extractCategory(relativePath);
                String relPathStr = relativePath.toString();

                Map<String, Object> result = blogSyncService.syncFile(mdFile, relPathStr, category);
                String action = (String) result.get("action");
                if ("skip".equals(action)) {
                    skipped++;
                } else {
                    synced++;
                    log.info("Blog sync [{}]: {} (category={}, blogId={})",
                            action, relPathStr, category, result.get("blogId"));
                }
            } catch (Exception e) {
                log.error("Failed to sync blog file: {}", mdFile, e);
            }
        }
        if (synced > 0 || skipped > 0) {
            log.info("Blog scan complete: {} synced, {} skipped", synced, skipped);
        }
    }

    private String extractCategory(Path relativePath) {
        if (relativePath.getNameCount() > 1) {
            String dirName = relativePath.getName(0).toString();
            // 用目录名去 category 表匹配 code 或 name
            Category cat = categoryService.getByCode(dirName);
            if (cat == null) {
                cat = categoryService.lambdaQuery().eq(Category::getName, dirName).one();
            }
            if (cat != null) {
                return cat.getCode();
            }
            return dirName;
        }
        return "未分类";
    }
}
