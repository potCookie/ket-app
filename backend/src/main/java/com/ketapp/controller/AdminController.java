package com.ketapp.controller;

import com.ketapp.common.Result;
import com.ketapp.config.LlmConfig;
import com.ketapp.service.AudioService;
import com.ketapp.service.ContentGeneratorService;
import com.ketapp.service.ScheduledContentTask;
import com.ketapp.entity.Task;
import com.ketapp.mapper.TaskMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TaskMapper taskMapper;
    private final AudioService audioService;
    private final ContentGeneratorService contentGenerator;
    private final ScheduledContentTask scheduledTask;
    private final LlmConfig llmConfig;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Regenerate all audio files from existing listening data.
     * POST /api/admin/regenerate-audio
     */
    @PostMapping("/regenerate-audio")
    public Result<String> regenerateAudio() {
        List<Task> tasks = taskMapper.selectList(null);
        int total = 0;
        int success = 0;

        for (Task task : tasks) {
            try {
                if (task.getListeningData() != null && !task.getListeningData().isEmpty()) {
                    JsonNode listening = objectMapper.readTree(task.getListeningData());
                    if (listening.has("questions")) {
                        for (JsonNode q : listening.get("questions")) {
                            if (q.has("audio_text")) {
                                total++;
                                try {
                                    audioService.generateAudio(q.get("audio_text").asText());
                                    success++;
                                } catch (Exception e) {
                                    log.warn("Failed audio: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse listening data for task {}: {}", task.getId(), e.getMessage());
            }
        }

        return Result.ok("Success: " + success + "/" + total + " audio files");
    }

    /**
     * Generate all 35 days of content (clears existing data first).
     * POST /api/admin/generate-all
     */
    @PostMapping("/generate-all")
    public Result<String> generateAll() {
        // Clear existing tasks
        taskMapper.delete(null);
        log.info("Cleared all existing tasks. Generating 35-day content...");

        LocalDate today = LocalDate.now();
        int count = contentGenerator.generateAll(today);
        return Result.ok("Generated " + count + " tasks starting from " + today);
    }

    /**
     * Generate tomorrow's content if not exists.
     * POST /api/admin/generate-next
     */
    @PostMapping("/generate-next")
    public Result<String> generateNext() {
        scheduledTask.dailyContentGeneration();
        return Result.ok("Content generation check completed");
    }

    /**
     * Get content generation status: how many days generated out of 35.
     * GET /api/admin/content-status
     */
    @GetMapping("/content-status")
    public Result<?> contentStatus() {
        long total = taskMapper.selectCount(null);
        var status = new java.util.HashMap<String, Object>();
        status.put("generated", total);
        status.put("total", 35);
        status.put("remaining", 35 - total);
        return Result.ok(status);
    }

    // ==================== LLM Configuration ====================

    /**
     * Get current LLM config status (key is masked).
     * GET /api/admin/llm-config
     */
    @GetMapping("/llm-config")
    public Result<?> getLlmConfig() {
        var config = new java.util.HashMap<String, Object>();
        config.put("enabled", llmConfig.isEnabled());
        config.put("configured", llmConfig.isConfigured());
        config.put("apiUrl", llmConfig.getApiUrl());
        config.put("model", llmConfig.getModel());
        // Mask the key: show first 8 chars
        String key = llmConfig.getApiKey();
        String masked = key != null && key.length() > 8
                ? key.substring(0, 8) + "..." : (key != null ? "***" : "(not set)");
        config.put("apiKey", masked);
        return Result.ok(config);
    }

    /**
     * Configure LLM API key at runtime.
     * POST /api/admin/llm-config
     * Body: {"enabled": true, "apiKey": "sk-...", "apiUrl": "https://...", "model": "gpt-4o-mini"}
     */
    @PostMapping("/llm-config")
    public Result<?> updateLlmConfig(@RequestBody Map<String, Object> body) {
        if (body.containsKey("enabled")) {
            llmConfig.setEnabled((Boolean) body.get("enabled"));
        }
        if (body.containsKey("apiKey")) {
            String key = (String) body.get("apiKey");
            if (key != null && !key.isBlank()) {
                llmConfig.setApiKey(key);
            }
        }
        if (body.containsKey("apiUrl")) {
            llmConfig.setApiUrl((String) body.get("apiUrl"));
        }
        if (body.containsKey("model")) {
            llmConfig.setModel((String) body.get("model"));
        }

        log.info("LLM config updated: enabled={}, model={}", llmConfig.isEnabled(), llmConfig.getModel());
        return Result.ok("LLM configuration updated. Re-run generation to use new settings.");
    }

    // ==================== Database Reset ====================

    /**
     * Truncate ALL user data: tasks, study_logs, recordings, badges, users, plan_config.
     * Only keeps the schema intact. Use with caution!
     * POST /api/admin/reset-database
     */
    @PostMapping("/reset-database")
    public Result<?> resetDatabase() {
        log.warn("=== RESETTING DATABASE ===");
        // Truncate in correct order to respect foreign key constraints
        jdbcTemplate.execute("DELETE FROM badge");
        jdbcTemplate.execute("DELETE FROM recording");
        jdbcTemplate.execute("DELETE FROM study_log");
        jdbcTemplate.execute("DELETE FROM plan_config");
        jdbcTemplate.execute("DELETE FROM task");
        jdbcTemplate.execute("DELETE FROM user");
        log.info("All user data cleared.");

        // Also clean up audio files
        try {
            java.io.File audioDir = new java.io.File("./audio");
            if (audioDir.exists()) {
                for (java.io.File f : audioDir.listFiles()) {
                    f.delete();
                }
            }
            // recordings subdir
            java.io.File recDir = new java.io.File("./audio/recordings");
            if (recDir.exists()) {
                for (java.io.File f : recDir.listFiles()) {
                    f.delete();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean audio files: {}", e.getMessage());
        }

        return Result.ok("Database reset complete. All data cleared. Restarting...");
    }

    /**
     * Clear study records only (users, logs, recordings, badges, plans).
     * Keeps task content intact so LLM doesn't need to re-generate.
     * POST /api/admin/reset-study-records
     */
    @PostMapping("/reset-study-records")
    public Result<?> resetStudyRecords() {
        log.warn("=== RESETTING STUDY RECORDS (keeping tasks) ===");
        jdbcTemplate.execute("DELETE FROM badge");
        jdbcTemplate.execute("DELETE FROM recording");
        jdbcTemplate.execute("DELETE FROM study_log");
        jdbcTemplate.execute("DELETE FROM plan_config");
        jdbcTemplate.execute("DELETE FROM user");
        log.info("Study records cleared. Tasks preserved.");

        // Clean up recording files only (not generated audio content)
        try {
            java.io.File recDir = new java.io.File("./audio/recordings");
            if (recDir.exists()) {
                for (java.io.File f : recDir.listFiles()) {
                    f.delete();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean recording files: {}", e.getMessage());
        }

        return Result.ok("Study records cleared. Tasks kept intact.");
    }

    // ==================== Task Generation Recovery ====================

    /**
     * Get list of failed tasks from scheduled generation.
     * GET /api/admin/failed-tasks
     */
    @GetMapping("/failed-tasks")
    public Result<?> getFailedTasks() {
        var failedTasks = scheduledTask.getFailedTasks();
        var list = new java.util.ArrayList<>();
        for (var record : failedTasks) {
            var item = new java.util.HashMap<String, Object>();
            item.put("date", record.getDate());
            item.put("dayNumber", record.getDayNumber());
            item.put("errorMsg", record.getErrorMsg());
            item.put("retryCount", record.getRetryCount());
            list.add(item);
        }
        return Result.ok(list);
    }

    /**
     * Manually retry failed tasks.
     * POST /api/admin/retry-failed-tasks
     */
    @PostMapping("/retry-failed-tasks")
    public Result<?> retryFailedTasks() {
        int success = scheduledTask.retryFailedTasks();
        return Result.ok("重试完成 - 成功: " + success);
    }

    /**
     * Clear failed tasks records.
     * POST /api/admin/clear-failed-tasks
     */
    @PostMapping("/clear-failed-tasks")
    public Result<?> clearFailedTasks() {
        scheduledTask.clearFailedTasks();
        return Result.ok("已清空所有失败任务记录");
    }

    /**
     * Generate content for specific days (e.g., missing days).
     * POST /api/admin/generate-days
     * Body: {"days": [1, 2, 3, 5]}
     */
    @PostMapping("/generate-days")
    public Result<?> generateDays(@RequestBody Map<String, Object> body) {
        Object daysObj = body.get("days");
        int[] days;
        
        if (daysObj instanceof List) {
            List<?> list = (List<?>) daysObj;
            days = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                days[i] = ((Number) list.get(i)).intValue();
            }
        } else {
            return Result.error("days 参数必须是数组");
        }

        scheduledTask.generateAllRemaining(); // Use the same logic
        return Result.ok("已开始生成指定天数的内容，查看日志获取详情");
    }
}
