package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.service.StudyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final StudyService studyService;

    @PostMapping
    public Result<String> checkIn(@RequestParam String date,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        studyService.checkIn(userId, LocalDate.parse(date));
        return Result.ok("打卡成功");
    }
}
