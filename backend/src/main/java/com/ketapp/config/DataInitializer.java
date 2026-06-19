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
 * On startup: runs DB migrations, then generates initial content if needed.
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
        // Run migrations
        runMigrations();

        long count = taskMapper.selectCount(null);
        if (count > 0) {
            log.info("Database already has {} tasks, skipping initial generation.", count);
            return;
        }

        log.info("=== First startup: generating today + next day ===");
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= 3; day++) {
            LocalDate date = today.plusDays(day - 1);
            try {
                contentGenerator.ensureToday(date, day);
            } catch (Exception e) {
                log.warn("Failed to generate day {}: {}", day, e.getMessage());
            }
        }
        log.info("=== Initial generation complete. Daily scheduler will handle the rest. ===");
    }

    private void runMigrations() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Ensure recording table exists
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
                log.debug("Migration: recording table may already exist: {}", e1.getMessage());
            }

            // Add quiz_answers column
            try {
                stmt.execute("ALTER TABLE study_log ADD COLUMN IF NOT EXISTS quiz_answers TEXT COMMENT 'JSON: user selected answer indices'");
            } catch (Exception e1) {
                try {
                    stmt.execute("ALTER TABLE study_log ADD COLUMN quiz_answers TEXT COMMENT 'JSON: user selected answer indices'");
                } catch (Exception e2) {
                    log.debug("Migration: quiz_answers column may already exist: {}", e2.getMessage());
                }
            }
            log.info("DB migration completed.");
        } catch (Exception e) {
            log.debug("Migration note: {}", e.getMessage());
        }
    }
}
