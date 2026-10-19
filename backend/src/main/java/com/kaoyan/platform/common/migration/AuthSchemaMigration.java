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
}
