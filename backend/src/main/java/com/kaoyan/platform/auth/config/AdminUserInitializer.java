package com.kaoyan.platform.auth.config;

import com.kaoyan.platform.user.entity.User;
import com.kaoyan.platform.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 独立的管理员引导：不依赖 demo 账号。
 * 生产设置 ADMIN_USERNAME / ADMIN_PASSWORD 后，启动时创建或提升管理员。
 */
@Slf4j
@Component
@Order(3)
public class AdminUserInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-user.username:admin}")
    private String username;

    @Value("${app.admin-user.password:}")
    private String password;

    @Value("${app.admin-user.required:false}")
    private boolean required;

    public AdminUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(password)) {
            if (required) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD 未配置：生产环境必须显式设置管理员密码");
            }
            log.info("未配置 ADMIN_PASSWORD，跳过管理员引导");
            return;
        }
        if (!username.matches("[A-Za-z0-9_]{3,50}")) {
            throw new IllegalStateException("ADMIN_USERNAME 必须是 3~50 位字母、数字或下划线");
        }
        if (password.length() < 8 || "learner123".equals(password)
                || password.contains("change-me")) {
            throw new IllegalStateException("ADMIN_PASSWORD 至少 8 位，且不能使用默认密码");
        }

        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            userMapper.updatePasswordAndRole(existing.getId(),
                    passwordEncoder.encode(password), "ADMIN");
            log.info("管理员 {} 已更新密码并确保 ADMIN 角色", username);
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("ADMIN");
        userMapper.insert(user);
        log.info("管理员 {} 已创建", username);
    }
}
