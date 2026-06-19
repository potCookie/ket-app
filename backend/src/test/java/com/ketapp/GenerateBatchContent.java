package com.ketapp;

import com.ketapp.service.ContentBatchGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDate;

/**
 * 生成指定日期的学习内容
 */
@SpringBootApplication
public class GenerateBatchContent {

    public static void main(String[] args) {
        // 假设计划从 2026-06-11 开始
        LocalDate startDate = LocalDate.of(2026, 6, 11);
        
        // 生成第 7 天和第 8 天的内容（对应 6 月 17 日和 18 日）
        int[] daysToGenerate = {7, 8};

        ConfigurableApplicationContext context = SpringApplication.run(GenerateBatchContent.class, args);
        
        ContentBatchGenerator generator = context.getBean(ContentBatchGenerator.class);
        
        // 打印课程大纲预览
        for (int day : daysToGenerate) {
            generator.printDayOutline(day);
        }
        
        // 生成内容
        generator.generateDays(startDate, daysToGenerate);
        
        context.close();
    }
}
