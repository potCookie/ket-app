package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.entity.Badge;
import com.ketapp.entity.StudyLog;
import com.ketapp.entity.User;
import com.ketapp.mapper.BadgeMapper;
import com.ketapp.mapper.StudyLogMapper;
import com.ketapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyLogMapper studyLogMapper;
    private final UserMapper userMapper;
    private final BadgeMapper badgeMapper;

    @Transactional
    public StudyLog startModule(Long userId, LocalDate taskDate, String module) {
        // Check if already started
        StudyLog existing = studyLogMapper.selectOne(
                new LambdaQueryWrapper<StudyLog>()
                        .eq(StudyLog::getUserId, userId)
                        .eq(StudyLog::getTaskDate, taskDate)
                        .eq(StudyLog::getModule, module));

        if (existing != null) {
            if (existing.getStartedAt() == null) {
                existing.setStartedAt(LocalDateTime.now());
                studyLogMapper.updateById(existing);
            }
            return existing;
        }

        StudyLog log = new StudyLog();
        log.setUserId(userId);
        log.setTaskDate(taskDate);
        log.setModule(module);
        log.setStartedAt(LocalDateTime.now());
        studyLogMapper.insert(log);
        return log;
    }

    @Transactional
    public StudyLog finishModule(Long userId, LocalDate taskDate, String module,
                                  Integer quizScore, Integer quizTotal, Integer starsEarned,
                                  String answers) {
        StudyLog log = studyLogMapper.selectOne(
                new LambdaQueryWrapper<StudyLog>()
                        .eq(StudyLog::getUserId, userId)
                        .eq(StudyLog::getTaskDate, taskDate)
                        .eq(StudyLog::getModule, module));

        if (log == null) {
            // Auto-start if not started
            log = startModule(userId, taskDate, module);
        }

        log.setFinishedAt(LocalDateTime.now());
        if (quizScore != null) log.setQuizScore(quizScore);
        if (quizTotal != null) log.setQuizTotal(quizTotal);
        if (answers != null) log.setQuizAnswers(answers);
        if (starsEarned != null) {
            log.setStarsEarned(starsEarned);
            // Add stars to user total
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setStars((user.getStars() != null ? user.getStars() : 0) + starsEarned);
                userMapper.updateById(user);
            }
        }

        studyLogMapper.updateById(log);

        // Check if all 5 modules finished → auto check-in
        checkAutoCheckIn(userId, taskDate);

        return log;
    }

    @Transactional
    public StudyLog submitQuiz(Long userId, LocalDate taskDate, String module,
                                Integer score, Integer total, String answers) {
        return finishModule(userId, taskDate, module, score, total, null, answers);
    }

    /**
     * Get study logs for a user on a specific date.
     */
    public List<StudyLog> getTodayLogs(Long userId, LocalDate taskDate) {
        return studyLogMapper.selectList(
                new LambdaQueryWrapper<StudyLog>()
                        .eq(StudyLog::getUserId, userId)
                        .eq(StudyLog::getTaskDate, taskDate));
    }

    /**
     * Check-in: mark all modules as checked_in, update streak.
     * Idempotent — only increments streak once per day.
     */
    @Transactional
    public void checkIn(Long userId, LocalDate taskDate) {
        List<StudyLog> logs = studyLogMapper.selectList(
                new LambdaQueryWrapper<StudyLog>()
                        .eq(StudyLog::getUserId, userId)
                        .eq(StudyLog::getTaskDate, taskDate));

        // Check if already checked in today
        boolean alreadyCheckedIn = logs.stream().anyMatch(l -> l.getCheckedIn() != null && l.getCheckedIn());

        for (StudyLog log : logs) {
            if (log.getCheckedIn() == null || !log.getCheckedIn()) {
                log.setCheckedIn(true);
                studyLogMapper.updateById(log);
            }
        }

        // Only increment streak once per day
        if (!alreadyCheckedIn) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setStreak((user.getStreak() != null ? user.getStreak() : 0) + 1);
                userMapper.updateById(user);
                checkBadges(user);
            }
        }
    }

    private void checkAutoCheckIn(Long userId, LocalDate taskDate) {
        List<StudyLog> logs = studyLogMapper.selectList(
                new LambdaQueryWrapper<StudyLog>()
                        .eq(StudyLog::getUserId, userId)
                        .eq(StudyLog::getTaskDate, taskDate));
        boolean allFinished = logs.size() == 5 && logs.stream().allMatch(l -> l.getFinishedAt() != null);
        if (allFinished && logs.stream().noneMatch(l -> l.getCheckedIn() != null && l.getCheckedIn())) {
            checkIn(userId, taskDate);
        }
    }

    private void checkBadges(User user) {
        int streak = user.getStreak() != null ? user.getStreak() : 0;
        List<Badge> badges = badgeMapper.selectList(
                new LambdaQueryWrapper<Badge>().eq(Badge::getUserId, user.getId()));
        boolean has7 = badges.stream().anyMatch(b -> "streak_7".equals(b.getBadgeType()));
        boolean has30 = badges.stream().anyMatch(b -> "streak_30".equals(b.getBadgeType()));

        if (streak >= 7 && !has7) {
            Badge badge = new Badge();
            badge.setUserId(user.getId());
            badge.setBadgeType("streak_7");
            badgeMapper.insert(badge);
        }
        if (streak >= 30 && !has30) {
            Badge badge = new Badge();
            badge.setUserId(user.getId());
            badge.setBadgeType("streak_30");
            badgeMapper.insert(badge);
        }
    }
}
