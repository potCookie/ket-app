package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.entity.PlanConfig;
import com.ketapp.entity.User;
import com.ketapp.mapper.PlanConfigMapper;
import com.ketapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanConfigService {

    private final PlanConfigMapper planConfigMapper;
    private final UserMapper userMapper;

    public PlanConfig getOrCreate(Long userId) {
        PlanConfig config = planConfigMapper.selectOne(
                new LambdaQueryWrapper<PlanConfig>().eq(PlanConfig::getUserId, userId));
        if (config == null) {
            User user = userMapper.selectById(userId);
            config = new PlanConfig();
            config.setUserId(userId);
            config.setStartDate(LocalDate.now().plusDays(1));
            config.setEndDate(LocalDate.now().plusDays(35));
            config.setTotalDays(35);
            config.setDailyDuration(user != null && user.getDailyGoal() != null ? user.getDailyGoal() : 35);
            planConfigMapper.insert(config);
        }
        return config;
    }

    public PlanConfig saveOrUpdate(Long userId, Map<String, Object> body) {
        PlanConfig config = planConfigMapper.selectOne(
                new LambdaQueryWrapper<PlanConfig>().eq(PlanConfig::getUserId, userId));

        if (config == null) {
            config = new PlanConfig();
            config.setUserId(userId);
        }

        if (body.containsKey("startDate")) {
            config.setStartDate(LocalDate.parse((String) body.get("startDate")));
        }
        if (body.containsKey("endDate")) {
            config.setEndDate(LocalDate.parse((String) body.get("endDate")));
        }
        if (body.containsKey("dailyDuration")) {
            config.setDailyDuration(((Number) body.get("dailyDuration")).intValue());
        }

        // Auto-calculate total days
        if (config.getStartDate() != null && config.getEndDate() != null) {
            long days = config.getEndDate().toEpochDay() - config.getStartDate().toEpochDay() + 1;
            config.setTotalDays((int) days);
        }

        if (config.getId() == null) {
            planConfigMapper.insert(config);
        } else {
            planConfigMapper.updateById(config);
        }

        // Also update user's daily_goal
        User user = userMapper.selectById(userId);
        if (user != null && body.containsKey("dailyDuration")) {
            user.setDailyGoal(config.getDailyDuration());
            userMapper.updateById(user);
        }

        return config;
    }
}
