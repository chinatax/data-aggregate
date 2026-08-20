package com.dataagg.start.controller;

import com.dataagg.common.result.Result;
import com.dataagg.common.entity.BookMonitorLog;
import com.dataagg.service.service.BlogService;
import com.dataagg.service.service.BookMonitorLogService;
import com.dataagg.service.service.UserService;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.cache.MultiLevelCache;
import com.dataagg.start.service.SchedulerTaskTracker;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class MonitorController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private MultiLevelCache cache;

    @Autowired
    private BlogService blogService;

    @Autowired
    private BookMonitorLogService bookMonitorLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private SchedulerTaskTracker schedulerTracker;

    @GetMapping("/monitor/recent")
    public Result<List<BookMonitorLog>> recentLogs() {
        List<BookMonitorLog> logs = bookMonitorLogService.lambdaQuery()
                .orderByDesc(BookMonitorLog::getScanTime)
                .last("LIMIT 4")
                .list();
        return Result.success(logs);
    }

    @GetMapping("/monitor")
    @RequireAuth("ADMIN")
    public Result<Map<String, Object>> monitor() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jvm", jvmMetrics());
        result.put("redis", redisMetrics());
        result.put("database", dbMetrics());
        result.put("cache", cache.getStats());
        result.put("system", systemMetrics());
        return Result.success(result);
    }

    private Map<String, Object> jvmMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        long heapUsed = rt.totalMemory() - rt.freeMemory();
        long heapMax = rt.maxMemory();
        m.put("heapUsed", formatBytes(heapUsed));
        m.put("heapMax", formatBytes(heapMax));
        m.put("heapUsedPercent", round(heapUsed * 100.0 / heapMax));

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long nonHeapUsed = memBean.getNonHeapMemoryUsage().getUsed();
        m.put("nonHeapUsed", formatBytes(nonHeapUsed));

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        m.put("threadCount", threadBean.getThreadCount());
        m.put("peakThreadCount", threadBean.getPeakThreadCount());

        long gcCount = 0, gcTimeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += gc.getCollectionCount();
            gcTimeMs += gc.getCollectionTime();
        }
        m.put("gcCount", gcCount);
        m.put("gcTimeMs", gcTimeMs);

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        m.put("availableProcessors", osBean.getAvailableProcessors());
        try {
            double load = osBean.getSystemLoadAverage();
            m.put("systemLoad", load >= 0 ? round(load) : "N/A");
        } catch (Exception e) {
            m.put("systemLoad", "N/A");
        }

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        m.put("uptime", formatDuration(uptimeMs));
        return m;
    }

    private Map<String, Object> redisMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            RedisConnection conn = redisConnectionFactory.getConnection();
            Properties info = conn.serverCommands().info();
            m.put("usedMemory", info.getProperty("used_memory_human", "N/A"));
            m.put("connectedClients", parseInt(info.getProperty("connected_clients")));
            m.put("keyspaceHits", parseInt(info.getProperty("keyspace_hits")));
            m.put("keyspaceMisses", parseInt(info.getProperty("keyspace_misses")));
            long hits = parseInt(info.getProperty("keyspace_hits"));
            long misses = parseInt(info.getProperty("keyspace_misses"));
            long total = hits + misses;
            m.put("hitRate", total > 0 ? round(hits * 100.0 / total) : 0);
            m.put("uptime", info.getProperty("uptime_in_seconds", "0") + "s");

            // count our cache keys
            try {
                Set<byte[]> keys = conn.keyCommands().keys("cache:*".getBytes());
                m.put("totalKeys", keys != null ? keys.size() : 0);
            } catch (Exception e) {
                m.put("totalKeys", "N/A");
            }
            conn.close();
        } catch (Exception e) {
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> dbMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            HikariDataSource hds = (HikariDataSource) dataSource;
            HikariPoolMXBean pool = hds.getHikariPoolMXBean();
            if (pool != null) {
                m.put("activeConnections", pool.getActiveConnections());
                m.put("idleConnections", pool.getIdleConnections());
                m.put("totalConnections", pool.getTotalConnections());
                m.put("waitingThreads", pool.getThreadsAwaitingConnection());
            }
        } catch (Exception e) {
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> systemMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("osName", System.getProperty("os.name"));
        m.put("osArch", System.getProperty("os.arch"));
        m.put("javaVersion", System.getProperty("java.version"));
        m.put("javaVendor", System.getProperty("java.vendor"));

        try {
            File root = new File("/app");
            if (!root.exists()) root = new File("/");
            m.put("totalDiskSpace", formatBytes(root.getTotalSpace()));
            m.put("freeDiskSpace", formatBytes(root.getFreeSpace()));
        } catch (Exception e) {
            m.put("diskError", e.getMessage());
        }

        try {
            m.put("blogCount", blogService.lambdaQuery().eq(
                    com.dataagg.common.entity.Blog::getDeleted, 0).count());
            m.put("userCount", userService.count());
        } catch (Exception e) {
            // ignore
        }
        return m;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return round(bytes / 1024.0) + " KB";
        if (bytes < 1024 * 1024 * 1024) return round(bytes / (1024.0 * 1024)) + " MB";
        return round(bytes / (1024.0 * 1024 * 1024)) + " GB";
    }

    private static String formatDuration(long ms) {
        long s = ms / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m";
        return m + "m " + (s % 60) + "s";
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static long parseInt(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0; }
    }

    @GetMapping("/monitor/schedulers")
    @RequireAuth("ADMIN")
    public Result<List<Map<String, Object>>> listSchedulers() {
        return Result.success(schedulerTracker.getAllStatuses());
    }

    @PostMapping("/monitor/scheduler/{taskId}/trigger")
    @RequireAuth("ADMIN")
    public Result<Void> triggerScheduler(@PathVariable String taskId) {
        try {
            schedulerTracker.trigger(taskId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
