package com.ketapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate taskDate;
    private Integer week;
    private Integer day;
    private String weekday;
    private String theme;
    private String duration;
    @TableField("vocab_data")
    private String vocabData;       // JSON stored as String
    @TableField("grammar_data")
    private String grammarData;
    @TableField("reading_data")
    private String readingData;
    @TableField("listening_data")
    private String listeningData;
    @TableField("speaking_data")
    private String speakingData;
    @TableField("writing_data")
    private String writingData;
    private String parentNote;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
