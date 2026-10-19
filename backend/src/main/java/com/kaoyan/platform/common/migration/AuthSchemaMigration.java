package com.kaoyan.platform.common.migration;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** v3 认证迁移：老库 user 表补 password_hash / role。 */
@Slf4j
@Component
@Order(1)
public class AuthSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public AuthSchemaMigration(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("user")) {
            log.warn("user 表不存在，跳过 v3 auth migration（请先执行 schema.sql）");
            return;
        }
        if (!columnExists("user", "password_hash")) {
            addColumn("ALTER TABLE `user` ADD COLUMN password_hash VARCHAR(100) NULL "
                    + "COMMENT 'BCrypt 密码哈希；v3 认证' AFTER username", "password_hash");
        }
        if (!columnExists("user", "role")) {
            addColumn("ALTER TABLE `user` ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' "
                    + "COMMENT 'USER / ADMIN' AFTER password_hash", "role");
        }
        if (!columnExists("user", "token_version")) {
            addColumn("ALTER TABLE `user` ADD COLUMN token_version INT NOT NULL DEFAULT 0 "
                    + "COMMENT '改密/登出时递增，使旧 JWT 失效' AFTER role", "token_version");
        }
        if (!columnExists("user", "email")) {
            addColumn("ALTER TABLE `user` ADD COLUMN email VARCHAR(120) NULL "
                    + "COMMENT '邮箱；用于验证与找回密码' AFTER token_version", "email");
        }
        if (!columnExists("user", "email_verified")) {
            addColumn("ALTER TABLE `user` ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0 "
                    + "COMMENT '邮箱是否已验证' AFTER email", "email_verified");
        }
        if (!indexExists("user", "uk_email")) {
            try {
                jdbc.execute("ALTER TABLE `user` ADD UNIQUE KEY uk_email (email)");
                log.info("v3 migration: user.uk_email 已添加");
            } catch (DataAccessException e) {
                log.warn("v3 migration: 添加 uk_email 失败：{}", e.getMessage());
            }
        }
        if (!tableExists("auth_token")) {
            try {
                jdbc.execute("""
                        CREATE TABLE auth_token (
                          id         BIGINT PRIMARY KEY AUTO_INCREMENT,
                          user_id    BIGINT NOT NULL,
                          type       VARCHAR(20) NOT NULL,
                          token_hash CHAR(64) NOT NULL,
                          expires_at DATETIME NOT NULL,
                          used_at    DATETIME NULL,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          UNIQUE KEY uk_token_hash (token_hash),
                          KEY idx_user_type (user_id, type),
                          KEY idx_expires (expires_at)
                        ) ENGINE = InnoDB COMMENT '一次性认证 token（只存 hash）'
                        """);
                log.info("v3 migration: auth_token 已创建");
            } catch (DataAccessException e) {
                log.warn("v3 migration: 创建 auth_token 失败：{}", e.getMessage());
            }
        }
    }

    private void addColumn(String sql, String name) {
        try {
            jdbc.execute(sql);
            log.info("v3 migration: user.{} 已添加", name);
        } catch (DataAccessException e) {
            log.warn("v3 migration: 添加 {} 失败（可能已存在）：{}", name, e.getMessage());
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
