package com.ketapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "角色不能为空")
    private String role;         // child / parent
    private String nickname;
    private String grade;
    private String target;
    private String inviteCode;   // 家长邀请码
    private Integer dailyGoal;
}
