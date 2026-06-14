package com.ketapp.dto;

import lombok.Data;

@Data
public class StudyFinishRequest {
    private String taskDate;
    private String module;
    private Integer quizScore;
    private Integer quizTotal;
    private Integer starsEarned;
    private String answers;  // JSON: {"0":0,"1":2}
}
