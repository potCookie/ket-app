package com.ketapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String role;         // child / parent
    private String nickname;
    private String grade;
    private String target;
    private String avatar;
    private String inviteCode;
    private Long parentId;
    private Integer stars;
    private Integer streak;
    private Integer dailyGoal;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
