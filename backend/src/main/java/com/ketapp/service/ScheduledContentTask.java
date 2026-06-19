package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.entity.Task;
import com.ketapp.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled task that runs daily to auto-generate KET learning content.
 * Checks each day if tomorrow's content exists, generates it if not.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledContentTask {

    private final TaskMapper taskMapper;
    private final ContentGeneratorService contentGenerator;

    // 失败任务记录，用于后续补生成
    private final List<FailedTaskRecord> failedTasks = new ArrayList<>();
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    /**
     * 记录失败任务的内部类
     */
    public static class FailedTaskRecord {
        private final LocalDate date;
        private final int dayNumber;
        private final String errorMsg;
        private final int retryCount;

        public FailedTaskRecord(LocalDate date, int dayNumber, String errorMsg, int retryCount) {
            this.date = date;
            this.dayNumber = dayNumber;
            this.errorMsg = errorMsg;
            this.retryCount = retryCount;
        }

        public LocalDate getDate() { return date; }
        public int getDayNumber() { return dayNumber; }
        public String getErrorMsg() { return errorMsg; }
        public int getRetryCount() { return retryCount; }
    }

    /**
     * Run every day at 3:00 AM to generate content for upcoming days.
     * Also checks if today's content needs generation (first-time setup).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void dailyContentGeneration() {
        log.info("=== Daily content generation started ===");
        LocalDate today = LocalDate.now();

        // Find the plan start date from existing tasks
        Task firstTask = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                .orderByAsc(Task::getTaskDate)
                .last("LIMIT 1"));

        LocalDate startDate;
        int todayDayNumber;

        if (firstTask != null) {
            startDate = firstTask.getTaskDate();
        } else {
            startDate = today;
        }

        todayDayNumber = (int) ChronoUnit.DAYS.between(startDate, today) + 1;

        if (todayDayNumber < 1) todayDayNumber = 1;
        if (todayDayNumber > 35) {
            log.info("35-day plan completed. No more content to generate.");
            return;
        }

        // Generate today if missing
        try {
            contentGenerator.ensureToday(today, todayDayNumber);
            consecutiveFailures.set(0); // 重置连续失败计数
        } catch (Exception e) {
            String errorMsg = "Failed to generate today's content: " + e.getMessage();
            log.error(errorMsg, e);
            failedTasks.add(new FailedTaskRecord(today, todayDayNumber, errorMsg, 1));
            consecutiveFailures.incrementAndGet();
        }

        // Pre-generate only the next day
        int nextDay = todayDayNumber + 1;
        if (nextDay <= 35) {
            LocalDate nextDate = today.plusDays(1);
            try {
                contentGenerator.ensureToday(nextDate, nextDay);
                consecutiveFailures.set(0); // 重置连续失败计数
            } catch (Exception e) {
                String errorMsg = "Failed to generate next day's content: " + e.getMessage();
                log.error(errorMsg, e);
                failedTasks.add(new FailedTaskRecord(nextDate, nextDay, errorMsg, 1));
                consecutiveFailures.incrementAndGet();
            }
        }

        // 检查是否连续失败过多
        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("⚠️ 连续 {} 次生成失败！已记录 {} 个失败任务", 
                consecutiveFailures.get(), failedTasks.size());
        }

        log.info("=== Daily content generation completed ===");
        log.info("成功: {}, 失败: {}", failedTasks.size() > 0 ? "有失败任务" : "全部成功", failedTasks.size());
    }

    /**
     * 手动触发补生成所有缺失的内容
     */
    public void generateAllRemaining() {
        LocalDate today = LocalDate.now();

        Task firstTask = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                .orderByAsc(Task::getTaskDate)
                .last("LIMIT 1"));

        LocalDate startDate = firstTask != null ? firstTask.getTaskDate() : today;
        int generated = 0;
        int failed = 0;

        for (int day = 1; day <= 35; day++) {
            LocalDate date = startDate.plusDays(day - 1);
            try {
                if (contentGenerator.generateDay(date, day)) {
                    generated++;
                }
            } catch (Exception e) {
                failed++;
                String errorMsg = "Failed to generate day " + day + ": " + e.getMessage();
                log.error(errorMsg, e);
                failedTasks.add(new FailedTaskRecord(date, day, errorMsg, 1));
            }
        }

        log.info("补生成完成 - 成功: {}, 失败: {}", generated, failed);
    }

    /**
     * 获取失败任务列表
     */
    public List<FailedTaskRecord> getFailedTasks() {
        return new ArrayList<>(failedTasks);
    }

    /**
     * 手动重试失败的任务
     */
    public int retryFailedTasks() {
        int retried = 0;
        int success = 0;
        
        for (FailedTaskRecord record : new ArrayList<>(failedTasks)) {
            retried++;
            try {
                boolean result = contentGenerator.generateDay(record.getDate(), record.getDayNumber());
                if (result) {
                    success++;
                    failedTasks.remove(record); // 移除成功记录
                }
            } catch (Exception e) {
                log.error("重试失败 - Day {}: {}", record.getDayNumber(), e.getMessage());
                // 更新重试次数
                int newRetryCount = record.getRetryCount() + 1;
                if (newRetryCount >= MAX_RETRY_ATTEMPTS) {
                    log.warn("Day {} 已重试 {} 次，不再重试", record.getDayNumber(), newRetryCount);
                    failedTasks.remove(record); // 超过最大重试次数，移除记录
                }
            }
        }
        
        log.info("重试完成 - 尝试: {}, 成功: {}", retried, success);
        return success;
    }

    /**
     * 清空失败任务记录
     */
    public void clearFailedTasks() {
        failedTasks.clear();
        consecutiveFailures.set(0);
        log.info("已清空所有失败任务记录");
    }
}
