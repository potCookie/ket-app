package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.dto.StatsResponse;
import com.ketapp.entity.Badge;
import com.ketapp.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/stats")
    public Result<StatsResponse> stats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(statsService.getStats(userId));
    }

    @GetMapping("/badges")
    public Result<List<Badge>> badges(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(statsService.getBadges(userId));
    }
}
