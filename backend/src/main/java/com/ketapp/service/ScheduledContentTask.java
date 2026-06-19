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

/**
 * 每日运行的计划任务，用于自动生成KET学习内容
 * 检查每天明天的内容是否存在，如果不存在则生成
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledContentTask {

    private final TaskMapper taskMapper;
    private final ContentGeneratorService contentGenerator;

    /**
     * 每天凌晨3点运行，为即将到来的日期生成内容
     * 同时检查今天的内容是否需要生成（首次设置）
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void dailyContentGeneration() {
        log.info("=== 开始每日内容生成 ===");
        LocalDate today = LocalDate.now();

        // 从现有任务中查找计划开始日期
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
            log.info("35天计划已完成。没有更多内容可生成。");
            return;
        }

        // 如果缺少今天的内容则生成
        contentGenerator.ensureToday(today, todayDayNumber);

        // 预生成下一天的内容
        int nextDay = todayDayNumber + 1;
        if (nextDay <= 35) {
            LocalDate nextDate = today.plusDays(1);
            contentGenerator.ensureToday(nextDate, nextDay);
        }

        log.info("=== 每日内容生成完成 ===");
    }

    /**
     * 手动触发：检查并生成所有剩余日期的内容
     */
    public void generateAllRemaining() {
        LocalDate today = LocalDate.now();

        Task firstTask = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>()
                .orderByAsc(Task::getTaskDate)
                .last("LIMIT 1"));

        LocalDate startDate = firstTask != null ? firstTask.getTaskDate() : today;
        int generated = 0;

        for (int day = 1; day <= 35; day++) {
            LocalDate date = startDate.plusDays(day - 1);
            if (contentGenerator.generateDay(date, day)) {
                generated++;
            }
        }

        log.info("生成了 {} 条新任务", generated);
    }
}
