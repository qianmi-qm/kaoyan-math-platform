-- ============================================================
-- 考研高等数学学习平台 v1 数据库结构（对应方案文档 04 章 ER 图）
-- 执行方式：docker-compose 首次启动自动执行；本地开发可手动 source
-- ============================================================

CREATE DATABASE IF NOT EXISTS kaoyan DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kaoyan;

CREATE TABLE IF NOT EXISTS `user` (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  username   VARCHAR(50) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT '用户（v1 单用户，预留多用户）';

CREATE TABLE IF NOT EXISTS question (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  type         TINYINT NOT NULL COMMENT '1单选 2填空 3大题',
  content      TEXT NOT NULL COMMENT '题干，LaTeX 原文',
  options      JSON NULL COMMENT '选择题选项，如 ["A. ...","B. ..."]',
  answer       TEXT NOT NULL COMMENT '正确答案',
  analysis     TEXT NULL COMMENT '解析，LaTeX 原文',
  difficulty   TINYINT NOT NULL DEFAULT 2 COMMENT '1易 2中 3难',
  content_hash CHAR(32) NOT NULL UNIQUE COMMENT 'MD5(题干)，批量导入幂等去重',
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
  duration_sec  INT NULL COMMENT '后端计算：submitted_at - started_at',
  started_at    DATETIME NOT NULL,
  submitted_at  DATETIME NULL,
  practice_date DATE NOT NULL,
  KEY idx_user_date (user_id, practice_date),
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
  KEY idx_user_due (user_id, next_review_date, status),
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

-- ---------- 初始数据 ----------
INSERT IGNORE INTO `user` (id, username) VALUES (1, 'learner');
INSERT IGNORE INTO knowledge_point (id, name, parent_id) VALUES
  (1, '极限', NULL),
  (2, '导数', NULL),
  (3, '积分', NULL),
  (4, '换元法', 3),
  (5, '分部积分', 3),
  (6, '洛必达法则', 1);
