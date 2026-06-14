package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.dto.StatsResponse;
import com.ketapp.entity.StudyLog;
import com.ketapp.entity.User;
import com.ketapp.service.ParentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @GetMapping("/children")
    public Result<List<User>> getChildren(HttpServletRequest request) {
        Long parentId = (Long) request.getAttribute("userId");
        return Result.ok(parentService.getChildren(parentId));
    }

    @GetMapping("/report")
    public Result<StatsResponse> getReport(@RequestParam Long childId,
                                            HttpServletRequest request) {
        Long parentId = (Long) request.getAttribute("userId");
        return Result.ok(parentService.getChildReport(parentId, childId));
    }

    @GetMapping("/records")
    public Result<List<StudyLog>> getRecords(@RequestParam Long childId,
                                              @RequestParam(required = false) String from,
                                              @RequestParam(required = false) String to,
                                              HttpServletRequest request) {
        Long parentId = (Long) request.getAttribute("userId");
        return Result.ok(parentService.getChildRecords(parentId, childId,
                from != null ? LocalDate.parse(from) : null,
                to != null ? LocalDate.parse(to) : null));
    }

    @PostMapping("/checkin")
    public Result<String> manualCheckIn(@RequestParam Long childId,
                                         @RequestParam String date,
                                         HttpServletRequest request) {
        Long parentId = (Long) request.getAttribute("userId");
        parentService.manualCheckIn(parentId, childId, LocalDate.parse(date));
        return Result.ok("手动打卡成功");
    }
}
