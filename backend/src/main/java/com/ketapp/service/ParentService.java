package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.dto.StatsResponse;
import com.ketapp.entity.StudyLog;
import com.ketapp.entity.User;
import com.ketapp.mapper.StudyLogMapper;
import com.ketapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final UserMapper userMapper;
    private final StudyLogMapper studyLogMapper;
    private final StatsService statsService;

    public List<User> getChildren(Long parentId) {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getParentId, parentId));
    }

    public StatsResponse getChildReport(Long parentId, Long childId) {
        // Verify parent-child relationship
        User child = userMapper.selectById(childId);
        if (child == null || !parentId.equals(child.getParentId())) {
            throw new RuntimeException("无权查看该孩子的报告");
        }
        return statsService.getStats(childId);
    }

    public List<StudyLog> getChildRecords(Long parentId, Long childId, LocalDate from, LocalDate to) {
        User child = userMapper.selectById(childId);
        if (child == null || !parentId.equals(child.getParentId())) {
            throw new RuntimeException("无权查看该孩子的记录");
        }

        LambdaQueryWrapper<StudyLog> wrapper = new LambdaQueryWrapper<StudyLog>()
                .eq(StudyLog::getUserId, childId);
        if (from != null) wrapper.ge(StudyLog::getTaskDate, from);
        if (to != null) wrapper.le(StudyLog::getTaskDate, to);
        wrapper.orderByDesc(StudyLog::getTaskDate);

        return studyLogMapper.selectList(wrapper);
    }

    public void manualCheckIn(Long parentId, Long childId, LocalDate date) {
        User child = userMapper.selectById(childId);
        if (child == null || !parentId.equals(child.getParentId())) {
            throw new RuntimeException("无权操作");
        }

        // Mark all modules for this date as checked_in
        List<StudyLog> logs = studyLogMapper.selectList(
                new LambdaQueryWrapper<StudyLog>()
                        .eq(StudyLog::getUserId, childId)
                        .eq(StudyLog::getTaskDate, date));
        for (StudyLog log : logs) {
            log.setCheckedIn(true);
            if (log.getFinishedAt() == null) {
                log.setFinishedAt(log.getStartedAt());
            }
            studyLogMapper.updateById(log);
        }
    }
}
