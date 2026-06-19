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
     * 上传录音文件
     * 返回文件URL
     */
    public String upload(Long userId, LocalDate taskDate, MultipartFile file) {
        log.info("收到录音上传请求: userId={}, date={}, 文件大小={}, 文件名={}",
                userId, taskDate, file.getSize(), file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new RuntimeException("录音文件为空");
        }

        try {
            // 确保录音目录存在（使用绝对路径）
            Path recDir = Paths.get(audioDir).toAbsolutePath().resolve("recordings");
            log.info("录音目录: {}", recDir);
            Files.createDirectories(recDir);

            // 生成唯一文件名
            String filename = UUID.randomUUID().toString() + ".webm";
            Path targetPath = recDir.resolve(filename);

            // 保存文件字节
            byte[] bytes = file.getBytes();
            Files.write(targetPath, bytes);
            log.info("录音已保存: {} ({} 字节)", targetPath, bytes.length);

            // 构建URL
            String url = "/api/recording/play/" + filename;

            // 保存到数据库
            Recording rec = new Recording();
            rec.setUserId(userId);
            rec.setTaskDate(taskDate);
            rec.setModule("speaking");
            rec.setFilePath(targetPath.toString());
            rec.setFileUrl(url);
            recordingMapper.insert(rec);
            log.info("录音记录已保存到数据库: id={}", rec.getId());

            return url;
        } catch (IOException e) {
            log.error("保存录音文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("录音保存失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("保存录音记录失败: {}", e.getMessage(), e);
            // 如果文件已写入但数据库插入失败，清理文件
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
