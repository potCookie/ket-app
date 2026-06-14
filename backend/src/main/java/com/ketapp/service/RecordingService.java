package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ketapp.entity.Recording;
import com.ketapp.mapper.RecordingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RecordingMapper recordingMapper;

    @Value("${ket.audio.dir:./audio}")
    private String audioDir;

    /**
     * Upload a recording blob for a user on a specific date.
     * Returns the file URL.
     */
    public String upload(Long userId, LocalDate taskDate, MultipartFile file) {
        log.info("Upload request: userId={}, date={}, fileSize={}, fileName={}",
                userId, taskDate, file.getSize(), file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new RuntimeException("录音文件为空");
        }

        try {
            // Ensure recordings directory exists (use absolute path)
            Path recDir = Paths.get(audioDir).toAbsolutePath().resolve("recordings");
            log.info("Recording directory: {}", recDir);
            Files.createDirectories(recDir);

            // Generate unique filename
            String filename = UUID.randomUUID().toString() + ".webm";
            Path targetPath = recDir.resolve(filename);

            // Save file bytes
            byte[] bytes = file.getBytes();
            Files.write(targetPath, bytes);
            log.info("Recording saved: {} ({} bytes)", targetPath, bytes.length);

            // Build URL
            String url = "/api/recording/play/" + filename;

            // Save to DB
            Recording rec = new Recording();
            rec.setUserId(userId);
            rec.setTaskDate(taskDate);
            rec.setModule("speaking");
            rec.setFilePath(targetPath.toString());
            rec.setFileUrl(url);
            recordingMapper.insert(rec);
            log.info("Recording record saved to DB: id={}", rec.getId());

            return url;
        } catch (IOException e) {
            log.error("Failed to save recording file: {}", e.getMessage(), e);
            throw new RuntimeException("录音保存失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to save recording record: {}", e.getMessage(), e);
            // Clean up the file if it was written but DB insert failed
            throw new RuntimeException("录音记录保存失败: " + e.getMessage());
        }
    }

    /**
     * Get the file path for a recording by filename.
     */
    public Path getRecordingFile(String filename) {
        return Paths.get(audioDir, "recordings", filename);
    }

    /**
     * Check if a recording file exists.
     */
    public boolean exists(String filename) {
        return Files.exists(getRecordingFile(filename));
    }

    /**
     * Get recordings for a user on a specific date.
     */
    public List<Recording> getByDate(Long userId, LocalDate date) {
        return recordingMapper.selectList(
                new LambdaQueryWrapper<Recording>()
                        .eq(Recording::getUserId, userId)
                        .eq(Recording::getTaskDate, date));
    }
}
