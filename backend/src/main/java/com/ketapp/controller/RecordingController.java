package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.entity.Recording;
import com.ketapp.service.RecordingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recording")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;

    /**
     * Upload a speaking recording.
     * POST /api/recording/upload
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam String date,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String url = recordingService.upload(userId, LocalDate.parse(date), file);
        return Result.ok(url);
    }

    /**
     * Play a recording by filename.
     * GET /api/recording/play/{filename}
     */
    @GetMapping("/play/{filename}")
    public ResponseEntity<Resource> play(@PathVariable String filename) {
        if (!recordingService.exists(filename)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(recordingService.getRecordingFile(filename));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/webm"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Get recordings for a specific date (defaults to today).
     * GET /api/recording/today?date=2026-06-14
     */
    @GetMapping("/today")
    public Result<List<Recording>> getToday(@RequestParam(required = false) String date,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return Result.ok(recordingService.getByDate(userId, targetDate));
    }
}
