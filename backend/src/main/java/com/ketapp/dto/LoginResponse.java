package com.ketapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String nickname;
    private String token;
    private Integer streak;
    private Integer stars;
    private String avatar;
}
