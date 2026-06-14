package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.common.JwtUtil;
import com.ketapp.dto.LoginRequest;
import com.ketapp.dto.LoginResponse;
import com.ketapp.dto.RegisterRequest;
import com.ketapp.entity.User;
import com.ketapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
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
                .avatar(user.getAvatar())
                .build();
    }

    /**
     * Upload and update user avatar. Returns the avatar URL.
     */
    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        // Delete old avatar file if exists
        if (user.getAvatar() != null && user.getAvatar().contains("/avatar/")) {
            String oldFile = user.getAvatar().substring(user.getAvatar().lastIndexOf('/') + 1);
            try {
                Files.deleteIfExists(Paths.get("avatars", oldFile));
            } catch (IOException e) {
                log.warn("Failed to delete old avatar: {}", e.getMessage());
            }
        }

        // Save new avatar
        try {
            Path avatarDir = Paths.get("avatars");
            Files.createDirectories(avatarDir);

            String ext = getExtension(file.getOriginalFilename());
            String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;
            Path targetPath = avatarDir.resolve(filename);
            Files.write(targetPath, file.getBytes());

            String url = "/api/auth/avatar/" + filename;
            user.setAvatar(url);
            userMapper.updateById(user);

            log.info("Avatar updated for user {}: {}", userId, url);
            return url;
        } catch (IOException e) {
            log.error("Failed to save avatar: {}", e.getMessage(), e);
            throw new RuntimeException("头像上传失败");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return ".png";
        return filename.substring(lastDot).toLowerCase();
    }
}
