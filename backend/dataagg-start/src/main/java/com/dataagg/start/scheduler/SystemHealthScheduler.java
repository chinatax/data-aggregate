package com.dataagg.start.scheduler;

import com.dataagg.start.service.SchedulerTaskTracker;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SystemHealthScheduler {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthScheduler.class);
    private static final long MIN_MAX_ALLOWED_PACKET = 1_048_576L;
    private static final long FIX_MAX_ALLOWED_PACKET = 67_108_864L;
    private static final long MIN_WAIT_TIMEOUT_SEC = 600L;
    private static final long MIN_BUFFER_POOL = 128L * 1024 * 1024;

    @Autowired
    private SchedulerTaskTracker tracker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${health.api-self-test.blog-id:1}")
    private long selfTestBlogId;

    @Value("${server.port:8081}")
    private int serverPort;

    @Value("${health.log-file.path:}")
    private String logFilePath;

    @Value("${health.log-file.max-size-gb:2}")
    private long logFileMaxSizeGb;

    @PostConstruct
    public void registerTasks() {
        tracker.register("checkMysqlConfig",
                "MySQL配置检查",
                "检查max_allowed_packet、wait_timeout等关键变量，max_allowed_packet<1MB时自动修正",
                "系统健康",
                "FIXED_DELAY",
                "1h",
                this::doCheckMysqlConfig);

        tracker.register("selfTestBlogApi",
                "API自测(博客详情)",
                "调用GET /api/blog/{id}验证返回200，检测因MySQL配置异常导致的隐蔽故障",
                "系统健康",
                "FIXED_DELAY",
                "5min",
                this::doSelfTestBlogApi);

        tracker.register("checkLogFileSize",
                "日志文件大小",
                "监控Docker容器日志大小，超过阈值时告警",
                "系统健康",
                "FIXED_DELAY",
                "30min",
                this::doCheckLogFileSize);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startupHealthCheck() {
        log.info("========== 启动时系统健康检查 开始 ==========");
        wrap("checkMysqlConfig", this::doCheckMysqlConfig);
        wrap("selfTestBlogApi", this::doSelfTestBlogApi);
        wrap("checkLogFileSize", this::doCheckLogFileSize);
        log.info("========== 启动时系统健康检查 完成 ==========");
    }

    @Scheduled(initialDelayString = "${health.mysql.initial-delay:120000}",
               fixedDelayString = "${health.mysql.check-interval:3600000}")
    public void checkMysqlConfig() {
        wrap("checkMysqlConfig", this::doCheckMysqlConfig);
    }

    @Scheduled(initialDelayString = "${health.api-self-test.initial-delay:60000}",
               fixedDelayString = "${health.api-self-test.interval:300000}")
    public void selfTestBlogApi() {
        wrap("selfTestBlogApi", this::doSelfTestBlogApi);
    }

    @Scheduled(initialDelayString = "${health.log-file.initial-delay:300000}",
               fixedDelayString = "${health.log-file.check-interval:1800000}")
    public void checkLogFileSize() {
        wrap("checkLogFileSize", this::doCheckLogFileSize);
    }

    private void doCheckMysqlConfig() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SHOW GLOBAL VARIABLES WHERE Variable_name IN " +
                "('max_allowed_packet', 'wait_timeout', 'innodb_buffer_pool_size')");

        Map<String, Long> vars = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("Variable_name");
            Object val = row.get("Value");
            vars.put(name, val == null ? 0L : Long.parseLong(val.toString()));
        }

        long maxPacket = vars.getOrDefault("max_allowed_packet", 0L);
        long waitTimeout = vars.getOrDefault("wait_timeout", 0L);
        long bufferPool = vars.getOrDefault("innodb_buffer_pool_size", 0L);

        log.info("MySQL GLOBAL: max_allowed_packet={} wait_timeout={}s buffer_pool={}",
                formatBytes(maxPacket), waitTimeout, formatBytes(bufferPool));

        StringBuilder issues = new StringBuilder();

        if (maxPacket < MIN_MAX_ALLOWED_PACKET) {
            String msg = String.format("max_allowed_packet=%s (<1MiB)，自动修正为64MiB",
                    formatBytes(maxPacket));
            log.warn(msg);
            issues.append(msg);
            try {
                jdbcTemplate.execute("SET GLOBAL max_allowed_packet=" + FIX_MAX_ALLOWED_PACKET);
                log.info("已执行: SET GLOBAL max_allowed_packet=64MiB");
            } catch (Exception e) {
                String fixMsg = String.format("自动修正失败: %s。请手动执行: SET GLOBAL max_allowed_packet=67108864;",
                        e.getMessage());
                log.error(fixMsg);
                issues.append("; ").append(fixMsg);
            }
            issues.append("; ");
        }

        if (waitTimeout < MIN_WAIT_TIMEOUT_SEC) {
            String msg = String.format("wait_timeout=%ds (<%ds) 可能致连接池断连",
                    waitTimeout, MIN_WAIT_TIMEOUT_SEC);
            log.warn(msg);
            issues.append(msg).append("; ");
        }

        if (bufferPool > 0 && bufferPool < MIN_BUFFER_POOL) {
            String msg = String.format("innodb_buffer_pool_size=%s (<128MiB) 影响查询性能",
                    formatBytes(bufferPool));
            log.warn(msg);
            issues.append(msg).append("; ");
        }

        if (issues.length() > 0) {
            throw new RuntimeException("MySQL配置存在异常: " + issues);
        }
    }

    private void doSelfTestBlogApi() {
        String url = "http://localhost:" + serverPort + "/api/blog/" + selfTestBlogId;
        try {
            @SuppressWarnings("rawtypes")
            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new RuntimeException("自测 GET " + url + " 返回null");
            }
            Object code = response.get("code");
            if (code instanceof Integer && (Integer) code == 200) {
                log.debug("API自测通过: {}", url);
                return;
            }
            String message = response.get("message") != null
                    ? response.get("message").toString() : "无错误信息";
            throw new RuntimeException(String.format("自测 GET %s -> code=%s, message=%s",
                    url, code, message));
        } catch (RestClientException e) {
            throw new RuntimeException(String.format("自测 GET %s HTTP错误: %s",
                    url, e.getMessage()), e);
        }
    }

    private void doCheckLogFileSize() {
        if (logFilePath == null || logFilePath.isBlank()) {
            return;
        }
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            log.warn("日志文件不存在: {}", logFilePath);
            return;
        }
        long sizeBytes = logFile.length();
        long maxBytes = logFileMaxSizeGb * 1024L * 1024L * 1024L;
        log.debug("日志文件: {} = {}", logFilePath, formatBytes(sizeBytes));
        if (sizeBytes > maxBytes) {
            String msg = String.format("Docker日志文件过大: %s (%.1f GiB, 阈值%d GiB)。"
                    + "建议: ssh服务器后执行 truncate -s 0 <日志路径> 或重启容器",
                    formatBytes(sizeBytes),
                    sizeBytes / (1024.0 * 1024 * 1024),
                    logFileMaxSizeGb);
            log.warn(msg);
            throw new RuntimeException(msg);
        }
    }

    private void wrap(String taskId, Runnable task) {
        tracker.markRunning(taskId);
        try {
            task.run();
            tracker.markSuccess(taskId);
        } catch (Exception e) {
            String errMsg = e.getMessage() != null
                    ? e.getMessage() : e.getClass().getSimpleName();
            tracker.markFailed(taskId, errMsg);
            log.error("{} 失败: {}", taskId, errMsg);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KiB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MiB", mb);
        double gb = mb / 1024.0;
        return String.format("%.1f GiB", gb);
    }
}
