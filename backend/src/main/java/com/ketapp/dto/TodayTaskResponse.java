package com.ketapp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TodayTaskResponse {
    private Long taskId;
    private LocalDate taskDate;
    private Integer week;
    private Integer day;
    private String weekday;
    private String theme;
    private String duration;
    // Module completion status
    private List<ModuleStatus> modules;

    @Data
    @Builder
    public static class ModuleStatus {
        private String module;        // vocab/reading/listening/speaking/grammar
        private String label;
        private String icon;
        private Boolean completed;
        private Integer score;
    }
}
