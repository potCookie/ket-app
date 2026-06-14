package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.common.JwtUtil;
import com.ketapp.dto.LoginRequest;
import com.ketapp.dto.LoginResponse;
import com.ketapp.dto.RegisterRequest;
import com.ketapp.entity.User;
import com.ketapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        // Check if username exists
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setGrade(req.getGrade());
        user.setTarget(req.getTarget() != null ? req.getTarget() : "KET");
        user.setDailyGoal(req.getDailyGoal() != null ? req.getDailyGoal() : 35);

        // If role is parent, generate invite code
        if ("parent".equals(req.getRole())) {
            user.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        }

        // If registering as child with invite code
        if ("child".equals(req.getRole()) && req.getInviteCode() != null && !req.getInviteCode().isEmpty()) {
            User parent = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getInviteCode, req.getInviteCode()));
            if (parent != null) {
                user.setParentId(parent.getId());
            }
        }

        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return buildLoginResponse(user, token);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return buildLoginResponse(user, token);
    }

    public LoginResponse getMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return buildLoginResponse(user, null);
    }

    private LoginResponse buildLoginResponse(User user, String token) {
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .nickname(user.getNickname())
                .token(token)
                .streak(user.getStreak() != null ? user.getStreak() : 0)
                .stars(user.getStars() != null ? user.getStars() : 0)
                .build();
    }
}
