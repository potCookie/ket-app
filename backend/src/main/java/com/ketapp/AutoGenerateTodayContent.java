package com.ketapp;

import com.ketapp.service.ContentBatchGenerator;
import com.ketapp.service.KetCurriculum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 启动时自动生成今天的学习内容
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoGenerateTodayContent implements CommandLineRunner {

    private final ContentBatchGenerator contentBatchGenerator;

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        log.info("=== Auto-generating content for today: {} ===", today);
        
        // 计算今天是第几天（从2026-06-13开始）
        LocalDate startDate = LocalDate.of(2026, 6, 13);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
        int dayNumber = (int) daysBetween + 1;
        
        log.info("Today is day {} of the curriculum", dayNumber);
        
        // 生成今天的内容
        contentBatchGenerator.generateDays(today, dayNumber);
        
        log.info("=== Content generation complete ===");
    }
}
