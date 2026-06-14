package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.dto.TodayTaskResponse;
import com.ketapp.entity.Task;
import com.ketapp.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/today")
    public Result<TodayTaskResponse> today(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(taskService.getToday(userId));
    }

    @GetMapping("/date")
    public Result<TodayTaskResponse> byDate(@RequestParam String date,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(taskService.getByDate(userId, LocalDate.parse(date)));
    }

    @GetMapping("/{id}")
    public Result<Task> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }
}
