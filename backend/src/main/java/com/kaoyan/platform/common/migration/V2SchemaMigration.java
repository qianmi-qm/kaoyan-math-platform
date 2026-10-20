package com.kaoyan.platform.common.migration;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * v2.0.1 兼容迁移：老库（v2.0.0 / v1）启动时补齐字段和唯一索引。
 * 新库由 schema.sql 直接建好，这里全部是幂等检查。
 */
@Slf4j
@Component
public class V2SchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public V2SchemaMigration(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("practice_record")) {
            log.warn("practice_record 不存在，跳过 v2 schema migration（请先执行 schema.sql）");
            return;
        }
        addSubmittedAnswerColumn();
        if (!tableExists("ai_explanation")) {
            createAiExplanationTable();
        } else {
            migrateAnswerHash();
        }
        createAiUsageTable();
        createPhotoSearchTable();
        migrateDailyReport();
    }

    private void migrateDailyReport() {
        if (!tableExists("daily_report")) {
            return;
        }
        addDailyReportColumn("submitted_count",
                "ALTER TABLE daily_report ADD COLUMN submitted_count INT NOT NULL DEFAULT 0 AFTER total_count");
        addDailyReportColumn("graded_count",
                "ALTER TABLE daily_report ADD COLUMN graded_count INT NOT NULL DEFAULT 0 AFTER submitted_count");
        addDailyReportColumn("overall_mastery",
                "ALTER TABLE daily_report ADD COLUMN overall_mastery DECIMAL(6,4) NOT NULL DEFAULT 0 AFTER avg_duration_sec");
        try {
            jdbc.update("""
                    UPDATE daily_report
                    SET submitted_count = total_count
                    WHERE submitted_count = 0 AND total_count > 0
                    """);
            jdbc.update("""
                    UPDATE daily_report
                    SET graded_count = submitted_count
                    WHERE graded_count = 0 AND submitted_count > 0
                    """);
        } catch (DataAccessException e) {
            log.warn("v2 migration: 回填 daily_report 计数失败：{}", e.getMessage());
        }
    }

    private void addDailyReportColumn(String name, String sql) {
        if (columnExists("daily_report", name)) {
            return;
        }
        try {
            jdbc.execute(sql);
            log.info("v2 migration: daily_report.{} 已添加", name);
        } catch (DataAccessException e) {
            log.warn("v2 migration: 添加 daily_report.{} 失败：{}", name, e.getMessage());
        }
    }

    private void addSubmittedAnswerColumn() {
        if (columnExists("practice_record", "submitted_answer")) {
            return;
        }
        try {
            jdbc.execute("ALTER TABLE practice_record ADD COLUMN submitted_answer TEXT NULL AFTER is_correct");
            log.info("v2 migration: practice_record.submitted_answer 已添加");
        } catch (DataAccessException e) {
            log.warn("v2 migration: 添加 submitted_answer 失败（可能已存在）：{}", e.getMessage());
        }
    }

    private void createAiExplanationTable() {
        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS ai_explanation (
                      id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
                      user_id            BIGINT NOT NULL,
                      question_id        BIGINT NOT NULL,
                      answer_hash        CHAR(64) NOT NULL COMMENT 'SHA-256(标准化用户答案)',
                      practice_record_id BIGINT NULL,
                      content            MEDIUMTEXT NOT NULL,
                      model              VARCHAR(64) NULL,
                      created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      UNIQUE KEY uk_user_question_answer (user_id, question_id, answer_hash),
                      KEY idx_question (question_id)
                    ) ENGINE = InnoDB COMMENT 'AI 错题讲解'
                    """);
            log.info("v2 migration: ai_explanation 已创建");
        } catch (DataAccessException e) {
            log.warn("v2 migration: 创建 ai_explanation 失败：{}", e.getMessage());
        }
    }

    private void createAiUsageTable() {
        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS ai_usage (
                      id             BIGINT PRIMARY KEY AUTO_INCREMENT,
                      user_id        BIGINT NOT NULL,
                      usage_date     DATE NOT NULL,
                      request_count  INT NOT NULL DEFAULT 0,
                      token_estimate INT NOT NULL DEFAULT 0,
                      UNIQUE KEY uk_user_date (user_id, usage_date),
                      KEY idx_date (usage_date)
                    ) ENGINE = InnoDB COMMENT 'AI 每日用量（配额 + 成本估算）'
                    """);
        } catch (DataAccessException e) {
            log.warn("v2 migration: 创建 ai_usage 失败：{}", e.getMessage());
        }
    }

    private void createPhotoSearchTable() {
        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS photo_search_record (
                      id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
                      user_id               BIGINT NOT NULL,
                      content               MEDIUMTEXT NOT NULL,
                      options_json          TEXT NULL,
                      answer                MEDIUMTEXT NULL,
                      steps_json            MEDIUMTEXT NULL,
                      knowledge_points_json VARCHAR(2000) NULL,
                      confidence            DECIMAL(3,2) NULL,
                      warnings_json         VARCHAR(2000) NULL,
                      model                 VARCHAR(64) NULL,
                      favorite              TINYINT(1) NOT NULL DEFAULT 0,
                      created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      KEY idx_user_created (user_id, created_at),
                      KEY idx_user_favorite (user_id, favorite)
                    ) ENGINE = InnoDB COMMENT '拍照搜题历史（只存识别文本，不存原图）'
                    """);
            log.info("v2 migration: photo_search_record 已创建");
        } catch (DataAccessException e) {
            log.warn("v2 migration: 创建 photo_search_record 失败：{}", e.getMessage());
        }
    }

    private void migrateAnswerHash() {
        if (!columnExists("ai_explanation", "answer_hash")) {
            try {
                jdbc.execute("ALTER TABLE ai_explanation ADD COLUMN answer_hash CHAR(64) NOT NULL DEFAULT '' "
                        + "COMMENT 'SHA-256(标准化用户答案)' AFTER question_id");
                jdbc.update("""
                        UPDATE ai_explanation ae
                        JOIN practice_record pr ON pr.id = ae.practice_record_id
                        SET ae.answer_hash = SHA2(LOWER(TRIM(COALESCE(pr.submitted_answer, ''))), 256)
                        WHERE ae.answer_hash = ''
                        """);
                log.info("v2 migration: ai_explanation.answer_hash 已添加并回填");
            } catch (DataAccessException e) {
                log.warn("v2 migration: 添加 answer_hash 失败：{}", e.getMessage());
            }
        }
        if (indexExists("ai_explanation", "uk_user_question")) {
            try {
                jdbc.execute("ALTER TABLE ai_explanation DROP INDEX uk_user_question");
                log.info("v2 migration: 旧唯一索引 uk_user_question 已删除");
            } catch (DataAccessException e) {
                log.warn("v2 migration: 删除旧唯一索引失败：{}", e.getMessage());
            }
        }
        if (!indexExists("ai_explanation", "uk_user_question_answer")) {
            try {
                jdbc.execute("ALTER TABLE ai_explanation ADD UNIQUE KEY uk_user_question_answer "
                        + "(user_id, question_id, answer_hash)");
                log.info("v2 migration: 新唯一索引 uk_user_question_answer 已添加");
            } catch (DataAccessException e) {
                log.warn("v2 migration: 添加新唯一索引失败：{}", e.getMessage());
            }
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean indexExists(String table, String index) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, table, index);
        return count != null && count > 0;
    }
}
