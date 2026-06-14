package com.ketapp.dto;

import lombok.Data;

@Data
public class QuizSubmitRequest {
    private String taskDate;
    private String module;
    private Integer score;
    private Integer total;
    private String answers;  // JSON: {"0":0,"1":2}
}
