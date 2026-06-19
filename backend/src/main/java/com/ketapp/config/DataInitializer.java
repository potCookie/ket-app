package com.ketapp.config;

import com.ketapp.entity.Task;
import com.ketapp.mapper.TaskMapper;
import com.ketapp.service.ContentGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * 启动时：执行数据库迁移，然后根据需要生成初始内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TaskMapper taskMapper;
    private final ContentGeneratorService contentGenerator;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        // 执行数据库迁移
        runMigrations();

        long count = taskMapper.selectCount(null);
        if (count > 0) {
            log.info("数据库已存在 {} 条任务，跳过初始生成。", count);
            return;
        }

        log.info("=== 首次启动：生成今天和明天的内容 ===");
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= 3; day++) {
            LocalDate date = today.plusDays(day - 1);
            try {
                contentGenerator.ensureToday(date, day);
            } catch (Exception e) {
                log.warn("生成第{}天内容失败: {}", day, e.getMessage());
            }
        }
        log.info("=== 初始生成完成。每日计划任务将处理其余内容。 ===");
    }

    private void runMigrations() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 确保recording表存在
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS recording (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "user_id BIGINT NOT NULL," +
                        "task_date DATE NOT NULL," +
                        "module VARCHAR(20) NOT NULL DEFAULT 'speaking'," +
                        "file_path VARCHAR(200)," +
                        "file_url VARCHAR(500)," +
                        "uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "INDEX idx_rec_user_task (user_id, task_date)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            } catch (Exception e1) {
                log.debug("迁移：recording表可能已存在: {}", e1.getMessage());
            }

            // 添加quiz_answers列
            try {
                stmt.execute("ALTER TABLE study_log ADD COLUMN IF NOT EXISTS quiz_answers TEXT COMMENT 'JSON: 用户选择的答案索引'");
            } catch (Exception e1) {
                try {
                    stmt.execute("ALTER TABLE study_log ADD COLUMN quiz_answers TEXT COMMENT 'JSON: 用户选择的答案索引'");
                } catch (Exception e2) {
                    log.debug("迁移：quiz_answers列可能已存在: {}", e2.getMessage());
                }
            }
            log.info("数据库迁移完成。");
        } catch (Exception e) {
            log.debug("迁移说明: {}", e.getMessage());
        }
    }
}
