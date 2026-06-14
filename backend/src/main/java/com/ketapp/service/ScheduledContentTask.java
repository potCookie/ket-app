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
 * Scheduled task that runs daily to auto-generate KET learning content.
 * Checks each day if tomorrow's content exists, generates it if not.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledContentTask {

    private final TaskMapper taskMapper;
    private final ContentGeneratorService contentGenerator;

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
        contentGenerator.ensureToday(today, todayDayNumber);

        // Pre-generate only the next day
        int nextDay = todayDayNumber + 1;
        if (nextDay <= 35) {
            LocalDate nextDate = today.plusDays(1);
            contentGenerator.ensureToday(nextDate, nextDay);
        }

        log.info("=== Daily content generation completed ===");
    }

    /**
     * Manual trigger: check and generate content for all remaining days
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

        log.info("Generated {} new tasks", generated);
    }
}
