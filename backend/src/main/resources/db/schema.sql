-- ============================================================
-- 考研高等数学学习平台 v1 数据库结构（对应方案文档 04 章 ER 图）
-- 执行方式：docker-compose 首次启动自动执行；本地开发可手动 source
-- ============================================================

CREATE DATABASE IF NOT EXISTS kaoyan DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kaoyan;
-- 保证客户端连接按 utf8mb4 解释脚本里的中文（Docker 初始化 / 本地 source 都需要）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NULL COMMENT 'BCrypt 密码哈希；v3 认证',
  role          VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
  token_version INT NOT NULL DEFAULT 0 COMMENT '改密/登出时递增，使旧 JWT 失效',
  email         VARCHAR(120) NULL COMMENT '邮箱；用于验证与找回密码',
  email_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_email (email)
) ENGINE = InnoDB COMMENT '用户（v3 起多用户，BCrypt 密码）';

CREATE TABLE IF NOT EXISTS auth_token (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  type       VARCHAR(20) NOT NULL COMMENT 'RESET_PASSWORD / VERIFY_EMAIL',
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at    DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_token_hash (token_hash),
  KEY idx_user_type (user_id, type),
  KEY idx_expires (expires_at)
) ENGINE = InnoDB COMMENT '一次性认证 token（只存 hash）';

CREATE TABLE IF NOT EXISTS question (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  type         TINYINT NOT NULL COMMENT '1单选 2填空 3大题',
  content      TEXT NOT NULL COMMENT '题干，LaTeX 原文',
  options      JSON NULL COMMENT '选择题选项，如 ["A. ...","B. ..."]',
  answer       TEXT NOT NULL COMMENT '正确答案',
  analysis     TEXT NULL COMMENT '解析，LaTeX 原文',
  difficulty   TINYINT NOT NULL DEFAULT 2 COMMENT '1易 2中 3难',
  content_hash CHAR(64) NOT NULL UNIQUE COMMENT 'SHA-256(题干)，批量导入幂等去重',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT '题目';

CREATE TABLE IF NOT EXISTS knowledge_point (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(100) NOT NULL UNIQUE,
  parent_id  BIGINT NULL COMMENT '父知识点，构成树形结构',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_kp_parent FOREIGN KEY (parent_id) REFERENCES knowledge_point (id)
) ENGINE = InnoDB COMMENT '知识点';

CREATE TABLE IF NOT EXISTS question_kp (
  question_id BIGINT NOT NULL,
  kp_id       BIGINT NOT NULL,
  PRIMARY KEY (question_id, kp_id),
  KEY idx_kp_question (kp_id, question_id),
  CONSTRAINT fk_qkp_question FOREIGN KEY (question_id) REFERENCES question (id),
  CONSTRAINT fk_qkp_kp FOREIGN KEY (kp_id) REFERENCES knowledge_point (id)
) ENGINE = InnoDB COMMENT '题目-知识点 多对多关联';

CREATE TABLE IF NOT EXISTS practice_record (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  question_id   BIGINT NOT NULL,
  is_correct    TINYINT(1) NULL COMMENT '1对 0错；大题未自评时为 NULL',
  submitted_answer TEXT NULL COMMENT '用户提交的答案，AI 讲解用',
  duration_sec  INT NULL COMMENT '后端计算：submitted_at - started_at',
  started_at    DATETIME NOT NULL,
  submitted_at  DATETIME NULL,
  practice_date DATE NOT NULL,
  KEY idx_user_date (user_id, practice_date),
  KEY idx_user_correct (user_id, is_correct, question_id),
  KEY idx_question (question_id),
  CONSTRAINT fk_pr_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE = InnoDB COMMENT '做题记录（细粒度留痕，只增不删）';

CREATE TABLE IF NOT EXISTS review_schedule (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT NOT NULL,
  question_id      BIGINT NOT NULL,
  stage            TINYINT NOT NULL DEFAULT 0 COMMENT '当前周期下标 0~4',
  next_review_date DATE NOT NULL,
  status           TINYINT NOT NULL DEFAULT 0 COMMENT '0待复习 1已完成',
  completed_at     DATETIME NULL,
  active_key       VARCHAR(64) GENERATED ALWAYS AS (
                     CASE WHEN status = 0 THEN CONCAT(user_id, ':', question_id) ELSE NULL END
                   ) STORED COMMENT 'active 唯一键；完成后为 NULL，允许重新入队',
  KEY idx_user_due (user_id, status, next_review_date),
  UNIQUE KEY uk_active (active_key),
  KEY idx_question (question_id)
) ENGINE = InnoDB COMMENT '艾宾浩斯复习队列（周期 1/2/4/7/15 天）';

CREATE TABLE IF NOT EXISTS daily_report (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT NOT NULL,
  report_date      DATE NOT NULL,
  total_count      INT NOT NULL DEFAULT 0,
  correct_count    INT NOT NULL DEFAULT 0,
  avg_duration_sec INT NOT NULL DEFAULT 0,
  kp_mastery       JSON NULL COMMENT '知识点掌握度快照 [{kpId,name,mastery}]',
  weak_points      JSON NULL COMMENT '薄弱点 [{kpId,name,mastery,count}]',
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, report_date)
) ENGINE = InnoDB COMMENT '每日报告（生成时刻的快照，永久保留）';

CREATE TABLE IF NOT EXISTS ai_explanation (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id            BIGINT NOT NULL,
  question_id        BIGINT NOT NULL,
  answer_hash        CHAR(64) NOT NULL COMMENT 'SHA-256(标准化用户答案)，不同错答分别讲解',
  practice_record_id BIGINT NULL COMMENT '首次生成讲解的记录',
  content            MEDIUMTEXT NOT NULL COMMENT 'LLM 生成的讲解全文',
  model              VARCHAR(64) NULL COMMENT '生成所用模型名',
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_question_answer (user_id, question_id, answer_hash),
  KEY idx_question (question_id)
) ENGINE = InnoDB COMMENT 'AI 错题讲解（同一用户同一题同一答案复用，控制 token）';

CREATE TABLE IF NOT EXISTS ai_usage (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  usage_date     DATE NOT NULL,
  request_count  INT NOT NULL DEFAULT 0,
  token_estimate INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_date (user_id, usage_date),
  KEY idx_date (usage_date)
) ENGINE = InnoDB COMMENT 'AI 每日用量（配额 + 成本估算）';

CREATE TABLE IF NOT EXISTS photo_search_record (
  id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id                BIGINT NOT NULL,
  content                MEDIUMTEXT NOT NULL,
  options_json           TEXT NULL,
  answer                 MEDIUMTEXT NULL,
  steps_json             MEDIUMTEXT NULL,
  knowledge_points_json  VARCHAR(2000) NULL,
  confidence             DECIMAL(3,2) NULL,
  warnings_json          VARCHAR(2000) NULL,
  model                  VARCHAR(64) NULL,
  favorite               TINYINT(1) NOT NULL DEFAULT 0,
  created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_created (user_id, created_at),
  KEY idx_user_favorite (user_id, favorite)
) ENGINE = InnoDB COMMENT '拍照搜题历史（只存识别文本，不存原图）';

-- ---------- 初始数据 ----------
INSERT IGNORE INTO `user` (id, username) VALUES (1, 'learner');
INSERT IGNORE INTO knowledge_point (id, name, parent_id) VALUES
  (1, '极限', NULL),
  (2, '导数', NULL),
  (3, '积分', NULL),
  (4, '换元法', 3),
  (5, '分部积分', 3),
  (6, '洛必达法则', 1);
