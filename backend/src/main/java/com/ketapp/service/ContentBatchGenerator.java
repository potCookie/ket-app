package com.ketapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 批量生成指定日期的学习内容
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentBatchGenerator {

    private final ContentGeneratorService contentGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Random RANDOM = ThreadLocalRandom.current();

    /**
     * 生成指定日期的学习内容
     * @param startDate 计划开始日期
     * @param dayNumbers 要生成的天数列表（1-35）
     */
    public void generateDays(LocalDate startDate, int... dayNumbers) {
        for (int day : dayNumbers) {
            if (day < 1 || day > 35) {
                log.warn("Day {} is outside the valid range (1-35)", day);
                continue;
            }
            
            LocalDate date = startDate.plusDays(day - 1);
            boolean success = contentGenerator.generateDay(date, day);
            
            if (success) {
                log.info("Successfully generated content for Day {} ({})", day, date);
            } else {
                log.info("Content for Day {} ({}) already exists or failed", day, date);
            }
        }
    }

    /**
     * 打印某一天的课程大纲（用于预览）
     */
    public void printDayOutline(int dayNumber) {
        int weekIdx = KetCurriculum.getWeekIndex(dayNumber);
        int dayIdx = KetCurriculum.getDayInWeekIndex(dayNumber);

        String[] weekTheme = KetCurriculum.getWeekTheme(weekIdx);
        String[] dailyTopic = KetCurriculum.getDailyTopic(weekIdx, dayIdx);
        String[][] vocab = KetCurriculum.getVocabulary(weekIdx, dayIdx);
        String[] grammar = KetCurriculum.getGrammar(weekIdx, dayIdx);

        System.out.println("\n========== 第 " + dayNumber + " 天课程大纲 ==========");
        System.out.println("主题: " + weekTheme[1] + " - " + dailyTopic[1]);
        System.out.println("语法点: " + grammar[0]);
        System.out.println("核心词汇 (10个):");
        for (String[] v : vocab) {
            System.out.println("  - " + v[0] + " (" + v[1] + ")");
        }
        System.out.println("=========================================\n");
    }
}
