package com.kaoyan.platform.common.migration;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** v3 认证迁移：老库 user 表补 password_hash。 */
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
        Integer table = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'user'
                """, Integer.class);
        if (table == null || table == 0) {
            log.warn("user 表不存在，跳过 v3 auth migration（请先执行 schema.sql）");
            return;
        }
        Integer column = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'password_hash'
                """, Integer.class);
        if (column != null && column > 0) {
            return;
        }
        try {
            jdbc.execute("ALTER TABLE `user` ADD COLUMN password_hash VARCHAR(100) NULL "
                    + "COMMENT 'BCrypt 密码哈希；v3 认证' AFTER username");
            log.info("v3 migration: user.password_hash 已添加");
        } catch (DataAccessException e) {
            log.warn("v3 migration: 添加 password_hash 失败（可能已存在）：{}", e.getMessage());
        }
    }
}
