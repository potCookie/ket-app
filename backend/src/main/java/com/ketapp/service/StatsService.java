package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ketapp.dto.StatsResponse;
import com.ketapp.entity.Badge;
import com.ketapp.entity.StudyLog;
import com.ketapp.entity.Task;
import com.ketapp.entity.User;
import com.ketapp.mapper.BadgeMapper;
import com.ketapp.mapper.StudyLogMapper;
import com.ketapp.mapper.TaskMapper;
import com.ketapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMapper userMapper;
    private final StudyLogMapper studyLogMapper;
    private final BadgeMapper badgeMapper;
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatsResponse getStats(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        List<StudyLog> logs = studyLogMapper.selectList(
                new LambdaQueryWrapper<StudyLog>().eq(StudyLog::getUserId, userId));

        // Completed days = distinct dates where check-in happened
        Set<LocalDate> checkedInDates = new HashSet<>();
        for (StudyLog log : logs) {
            if (log.getCheckedIn() != null && log.getCheckedIn()) {
                checkedInDates.add(log.getTaskDate());
            }
        }
        int completedDays = checkedInDates.size();

        // Longest streak: find max consecutive days in checkedInDates
        int longestStreak = computeLongestStreak(checkedInDates);

        // Accuracy
        int totalScores = logs.stream().mapToInt(l -> l.getQuizScore() != null ? l.getQuizScore() : 0).sum();
        int totalQuestions = logs.stream().mapToInt(l -> l.getQuizTotal() != null ? l.getQuizTotal() : 0).sum();
        double accuracy = totalQuestions > 0 ? ((double) totalScores / totalQuestions) * 100 : 0;

        // Vocab learned: count actual words from completed vocab modules
        int vocabLearned = countVocabLearned(logs);

        return StatsResponse.builder()
                .stars(user.getStars() != null ? user.getStars() : 0)
                .streakDays(user.getStreak() != null ? user.getStreak() : 0)
                .completedDays(completedDays)
                .totalDays(35)
                .accuracy(Math.round(accuracy * 10.0) / 10.0)
                .longestStreak(longestStreak)
                .learnedWords(vocabLearned)
                .build();
    }

    /**
     * Compute the longest consecutive streak of checked-in dates.
     */
    private int computeLongestStreak(Set<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        List<LocalDate> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);

        int longest = 1;
        int current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (ChronoUnit.DAYS.between(sorted.get(i - 1), sorted.get(i)) == 1) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    /**
     * Count actual vocabulary words learned by parsing completed vocab module data.
     */
    private int countVocabLearned(List<StudyLog> logs) {
        int total = 0;
        for (StudyLog log : logs) {
            if (!"vocab".equals(log.getModule()) || log.getFinishedAt() == null) continue;
            Task task = taskMapper.selectOne(
                    new LambdaQueryWrapper<Task>().eq(Task::getTaskDate, log.getTaskDate()));
            if (task == null || task.getVocabData() == null) continue;
            try {
                JsonNode vocabData = objectMapper.readTree(task.getVocabData());
                if (vocabData.has("words")) {
                    total += vocabData.get("words").size();
                }
            } catch (Exception e) {
                // skip malformed JSON
            }
        }
        return total;
    }

    public List<Badge> getBadges(Long userId) {
        return badgeMapper.selectList(
                new LambdaQueryWrapper<Badge>().eq(Badge::getUserId, userId));
    }
}
