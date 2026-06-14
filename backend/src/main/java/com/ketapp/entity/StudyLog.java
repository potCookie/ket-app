package com.ketapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_log")
public class StudyLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate taskDate;
    private String module;       // vocab/reading/listening/speaking/grammar
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer quizScore;
    private Integer quizTotal;
    @TableField("quiz_answers")
    private String quizAnswers;  // JSON: {"0":0,"1":2} - question index → selected option index
    private Boolean checkedIn;
    private Integer starsEarned;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
