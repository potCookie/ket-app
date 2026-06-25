package com.ketapp;

import com.ketapp.service.ContentBatchGenerator;
import com.ketapp.service.ContentGeneratorService;
import com.ketapp.service.KetCurriculum;
import com.ketapp.entity.Task;
import com.ketapp.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 启动时生成今天的学习内容
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateTodayContent implements CommandLineRunner {

    private final ContentGeneratorService contentGenerator;
    private final TaskMapper taskMapper;

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        log.info("=== 生成今天的学习内容: {} ===", today);

        // 计算今天是第几天（从2026-06-13开始）
        LocalDate startDate = LocalDate.of(2026, 6, 13);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
        int dayNumber = (int) daysBetween + 1;

        if (dayNumber < 1 || dayNumber > 35) {
            log.warn("今天是第{}天，超出有效范围(1-35)", dayNumber);
            return;
        }

        // 检查是否已存在
        long count = taskMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Task>()
                        .eq(Task::getTaskDate, today));
        
        if (count > 0) {
            log.info("今天的学习内容已存在，跳过生成");
            return;
        }

        // 生成今天的内容
        boolean success = contentGenerator.generateDay(today, dayNumber);
        
        if (success) {
            log.info("=== 成功生成第{}天的学习内容 ===", dayNumber);
        } else {
            log.warn("=== 内容生成失败或已存在 ===");
        }
    }
}
