package com.ketapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsResponse {
    private Integer stars;           // total stars
    private Integer streakDays;      // current streak
    private Integer completedDays;
    private Integer totalDays;
    private Double accuracy;          // 正确率 0-100
    private Integer longestStreak;    // longest consecutive streak
    private Integer learnedWords;     // vocabulary count
}
