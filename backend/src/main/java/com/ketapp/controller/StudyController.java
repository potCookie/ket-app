package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.dto.StudyFinishRequest;
import com.ketapp.dto.StudyStartRequest;
import com.ketapp.dto.QuizSubmitRequest;
import com.ketapp.entity.StudyLog;
import com.ketapp.service.StudyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @PostMapping("/start")
    public Result<StudyLog> start(@RequestBody StudyStartRequest req,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(studyService.startModule(userId, LocalDate.parse(req.getTaskDate()), req.getModule()));
    }

    @PostMapping("/finish")
    public Result<StudyLog> finish(@RequestBody StudyFinishRequest req,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(studyService.finishModule(userId,
                LocalDate.parse(req.getTaskDate()), req.getModule(),
                req.getQuizScore(), req.getQuizTotal(), req.getStarsEarned(),
                req.getAnswers()));
    }

    @PostMapping("/quiz/submit")
    public Result<StudyLog> submitQuiz(@RequestBody QuizSubmitRequest req,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(studyService.submitQuiz(userId,
                LocalDate.parse(req.getTaskDate()), req.getModule(),
                req.getScore(), req.getTotal(), req.getAnswers()));
    }

    /**
     * Get study logs for today (quiz scores + answers).
     * GET /api/study/today-logs
     */
    @GetMapping("/today-logs")
    public Result<java.util.List<StudyLog>> getTodayLogs(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(studyService.getTodayLogs(userId, LocalDate.now()));
    }

    /**
     * Get study logs for any date.
     * GET /api/study/logs?date=2026-06-14
     */
    @GetMapping("/logs")
    public Result<java.util.List<StudyLog>> getLogs(@RequestParam String date,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(studyService.getTodayLogs(userId, LocalDate.parse(date)));
    }

    /**
     * Get all studied dates with summary.
     * GET /api/study/history
     */
    @GetMapping("/history")
    public Result<java.util.List<java.util.Map<String, Object>>> getHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(studyService.getHistory(userId));
    }

    /**
     * Makeup study: start a module for a historical date.
     * POST /api/study/makeup
     */
    @PostMapping("/makeup")
    public Result<StudyLog> makeup(@RequestBody StudyStartRequest req,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(studyService.makeupStudy(userId,
                LocalDate.parse(req.getTaskDate()), req.getModule()));
    }
}
