-- KET App Database Schema
-- MySQL 8.0+

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt encrypted',
    role VARCHAR(10) NOT NULL DEFAULT 'child' COMMENT 'child / parent',
    nickname VARCHAR(50) COMMENT '孩子昵称',
    grade VARCHAR(20) COMMENT '年级',
    target VARCHAR(10) DEFAULT 'KET' COMMENT '备考目标',
    avatar VARCHAR(200) COMMENT '头像',
    invite_code VARCHAR(20) COMMENT '家长邀请码',
    parent_id BIGINT COMMENT '关联家长ID',
    stars INT DEFAULT 0 COMMENT '累计星星数',
    streak INT DEFAULT 0 COMMENT '连续打卡天数',
    daily_goal INT DEFAULT 35 COMMENT '每日学习时长(分钟)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `plan_config` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INT NOT NULL,
    daily_duration INT DEFAULT 35,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `task` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_date DATE NOT NULL UNIQUE,
    week INT NOT NULL,
    day INT NOT NULL,
    weekday VARCHAR(10),
    theme VARCHAR(100) NOT NULL,
    duration VARCHAR(20),
    vocab_data JSON,
    grammar_data JSON,
    reading_data JSON,
    listening_data JSON,
    speaking_data JSON,
    writing_data JSON,
    parent_note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_date (task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `study_log` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_date DATE NOT NULL,
    module VARCHAR(20) NOT NULL COMMENT 'vocab/reading/listening/speaking/grammar',
    started_at DATETIME,
    finished_at DATETIME,
    quiz_score INT DEFAULT 0,
    quiz_total INT DEFAULT 0,
    quiz_answers TEXT COMMENT 'JSON: user selected answer indices',
    checked_in BOOLEAN DEFAULT FALSE,
    stars_earned INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_task (user_id, task_date),
    UNIQUE KEY uk_user_task_module (user_id, task_date, module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `recording` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_date DATE NOT NULL,
    module VARCHAR(20) NOT NULL DEFAULT 'speaking',
    file_path VARCHAR(200),
    file_url VARCHAR(500),
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_task (user_id, task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `badge` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    badge_type VARCHAR(30) NOT NULL COMMENT 'streak_7/streak_30/perfect_attendance/full_score',
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_badge (user_id),
    UNIQUE KEY uk_user_badge (user_id, badge_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
