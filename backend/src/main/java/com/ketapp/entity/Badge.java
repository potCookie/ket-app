package com.ketapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("badge")
public class Badge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String badgeType;   // streak_7/streak_30/perfect_attendance/full_score
    private LocalDateTime earnedAt;
}
