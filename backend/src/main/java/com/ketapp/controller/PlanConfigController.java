package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.entity.PlanConfig;
import com.ketapp.service.PlanConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanConfigController {

    private final PlanConfigService planConfigService;

    @GetMapping
    public Result<PlanConfig> getPlan(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(planConfigService.getOrCreate(userId));
    }

    @PostMapping
    public Result<PlanConfig> savePlan(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(planConfigService.saveOrUpdate(userId, body));
    }
}
