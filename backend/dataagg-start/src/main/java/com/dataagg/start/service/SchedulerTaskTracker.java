package com.dataagg.start.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SchedulerTaskTracker {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTaskTracker.class);

    private final ConcurrentHashMap<String, TaskStatus> tasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Runnable> registry = new ConcurrentHashMap<>();

    public void register(String taskId, String name, String description, String group,
                         String triggerType, String triggerValue, Runnable runnable) {
        TaskStatus status = new TaskStatus();
        status.taskId = taskId;
        status.name = name;
        status.description = description;
        status.group = group;
        status.triggerType = triggerType;
        status.triggerValue = triggerValue;
        status.status = "IDLE";
        tasks.put(taskId, status);
        if (runnable != null) {
            registry.put(taskId, runnable);
        }
    }

    public void markRunning(String taskId) {
        TaskStatus s = tasks.get(taskId);
        if (s == null) return;
        s.status = "RUNNING";
        s.lastStartTime = LocalDateTime.now();
    }

    public void markSuccess(String taskId) {
        TaskStatus s = tasks.get(taskId);
        if (s == null) return;
        LocalDateTime now = LocalDateTime.now();
        s.lastEndTime = now;
        s.status = "SUCCESS";
        s.executionCount.incrementAndGet();
        if (s.lastStartTime != null) {
            s.duration = Duration.between(s.lastStartTime, now).toMillis();
        }
    }

    public void markFailed(String taskId, String errorMessage) {
        TaskStatus s = tasks.get(taskId);
        if (s == null) return;
        LocalDateTime now = LocalDateTime.now();
        s.lastEndTime = now;
        s.status = "FAILED";
        s.errorMessage = errorMessage;
        s.executionCount.incrementAndGet();
        s.totalFailures.incrementAndGet();
        if (s.lastStartTime != null) {
            s.duration = Duration.between(s.lastStartTime, now).toMillis();
        }
    }

    public List<Map<String, Object>> getAllStatuses() {
        List<Map<String, Object>> list = new ArrayList<>();
        tasks.values().stream()
                .sorted(Comparator.comparing((TaskStatus s) -> s.group != null ? s.group : "")
                        .thenComparing(s -> s.name))
                .forEach(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("taskId", s.taskId);
                    m.put("name", s.name);
                    m.put("description", s.description);
                    m.put("group", s.group);
                    m.put("triggerType", s.triggerType);
                    m.put("triggerValue", s.triggerValue);
                    m.put("status", s.status);
                    m.put("lastStartTime", s.lastStartTime != null ? s.lastStartTime.toString() : null);
                    m.put("lastEndTime", s.lastEndTime != null ? s.lastEndTime.toString() : null);
                    m.put("duration", s.duration != null ? formatDuration(s.duration) : null);
                    m.put("errorMessage", s.errorMessage);
                    m.put("executionCount", s.executionCount.get());
                    m.put("totalFailures", s.totalFailures.get());
                    list.add(m);
                });
        return list;
    }

    public void trigger(String taskId) {
        Runnable r = registry.get(taskId);
        if (r == null) {
            throw new IllegalArgumentException("Task not found or not triggerable: " + taskId);
        }
        log.info("Manual trigger: {}", taskId);
        CompletableFuture.runAsync(() -> {
            try {
                r.run();
            } catch (Exception e) {
                log.error("Manual trigger failed: {} - {}", taskId, e.getMessage());
            }
        });
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return (ms / 1000) + "s";
        long s = ms / 1000;
        return (s / 60) + "m " + (s % 60) + "s";
    }

    static class TaskStatus {
        String taskId;
        String name;
        String description;
        String group;
        String triggerType;
        String triggerValue;
        volatile String status;
        volatile LocalDateTime lastStartTime;
        volatile LocalDateTime lastEndTime;
        volatile Long duration;
        volatile String errorMessage;
        AtomicInteger executionCount = new AtomicInteger(0);
        AtomicInteger totalFailures = new AtomicInteger(0);
    }
}
