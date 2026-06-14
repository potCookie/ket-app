package com.ketapp.dto;

import lombok.Data;

@Data
public class StudyStartRequest {
    private String taskDate;    // yyyy-MM-dd
    private String module;      // vocab/reading/listening/speaking/grammar
}
