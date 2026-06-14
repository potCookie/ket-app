package com.ketapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("plan_config")
public class PlanConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer dailyDuration;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
