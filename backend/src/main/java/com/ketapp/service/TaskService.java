package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.dto.TodayTaskResponse;
import com.ketapp.entity.StudyLog;
import com.ketapp.entity.Task;
import com.ketapp.mapper.StudyLogMapper;
import com.ketapp.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final StudyLogMapper studyLogMapper;

    private static final String[][] MODULES = {
        {"vocab", "📚 词汇", "📚"},
        {"reading", "📖 阅读", "📖"},
        {"listening", "🎧 听力", "🎧"},
        {"speaking", "🗣️ 口语", "🗣️"},
        {"grammar", "✏️ 语法", "✏️"},
    };

    public TodayTaskResponse getToday(Long userId) {
        LocalDate today = LocalDate.now();
        return getByDate(userId, today);
    }

    public TodayTaskResponse getByDate(Long userId, LocalDate date) {
        Task task = taskMapper.selectOne(
                new LambdaQueryWrapper<Task>().eq(Task::getTaskDate, date));
        if (task == null) {
            // Fallback: find the latest available task (for demo/dev scenarios)
            task = taskMapper.selectOne(
                    new LambdaQueryWrapper<Task>()
                            .le(Task::getTaskDate, date)
                            .orderByDesc(Task::getTaskDate)
                            .last("LIMIT 1"));
        }
        if (task == null) {
            throw new RuntimeException("该日期暂无学习任务");
        }

        // Get study logs for this user and date
        List<StudyLog> logs = studyLogMapper.selectList(
                new LambdaQueryWrapper<StudyLog>()
                        .eq(StudyLog::getUserId, userId)
                        .eq(StudyLog::getTaskDate, date));
        Map<String, StudyLog> logMap = logs.stream()
                .collect(Collectors.toMap(StudyLog::getModule, log -> log, (a, b) -> b));

        List<TodayTaskResponse.ModuleStatus> modules = new ArrayList<>();
        for (String[] m : MODULES) {
            StudyLog log = logMap.get(m[0]);
            modules.add(TodayTaskResponse.ModuleStatus.builder()
                    .module(m[0])
                    .label(m[1])
                    .icon(m[2])
                    .completed(log != null && log.getFinishedAt() != null)
                    .score(log != null ? log.getQuizScore() : null)
                    .build());
        }

        return TodayTaskResponse.builder()
                .taskId(task.getId())
                .taskDate(task.getTaskDate())
                .week(task.getWeek())
                .day(task.getDay())
                .weekday(task.getWeekday())
                .theme(task.getTheme())
                .duration(task.getDuration())
                .modules(modules)
                .build();
    }

    /**
     * Get full task with all module data
     */
    public Task getById(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    /**
     * Get task by date
     */
    public Task getTaskByDate(LocalDate date) {
        Task task = taskMapper.selectOne(
                new LambdaQueryWrapper<Task>().eq(Task::getTaskDate, date));
        if (task == null) {
            task = taskMapper.selectOne(
                    new LambdaQueryWrapper<Task>()
                            .le(Task::getTaskDate, date)
                            .orderByDesc(Task::getTaskDate)
                            .last("LIMIT 1"));
        }
        if (task == null) {
            throw new RuntimeException("该日期暂无学习任务");
        }
        return task;
    }
}
