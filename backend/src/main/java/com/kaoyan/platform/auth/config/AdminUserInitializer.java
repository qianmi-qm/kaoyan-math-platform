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
 * 默认只创建/补齐密码，不覆盖管理员在页面修改后的密码；
 * 需要轮换时显式设置 ADMIN_PASSWORD_FORCE=true。
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

    @Value("${app.admin-user.required:true}")
    private boolean required;

    @Value("${app.admin-user.force-password:false}")
    private boolean forcePassword;

    public AdminUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!username.matches("[A-Za-z0-9_]{3,50}")) {
            throw new IllegalStateException("ADMIN_USERNAME 必须是 3~50 位字母、数字或下划线");
        }
        User existing = userMapper.selectByUsername(username);

        if (!StringUtils.hasText(password)) {
            if (existing != null && "ADMIN".equals(existing.getRole())
                    && StringUtils.hasText(existing.getPasswordHash())) {
                log.info("管理员 {} 已存在，未配置 ADMIN_PASSWORD，保留现有密码", username);
                return;
            }
            if (required) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD 未配置：生产环境必须显式设置管理员密码");
            }
            log.info("未配置 ADMIN_PASSWORD，跳过管理员引导");
            return;
        }
        if (password.length() < 8 || "learner123".equals(password)
                || password.contains("change-me")) {
            throw new IllegalStateException("ADMIN_PASSWORD 至少 8 位，且不能使用默认密码");
        }

        if (existing != null) {
            if (!"ADMIN".equals(existing.getRole())) {
                userMapper.updateRole(existing.getId(), "ADMIN");
            }
            if (!StringUtils.hasText(existing.getPasswordHash()) || forcePassword) {
                userMapper.updatePasswordAndRole(existing.getId(),
                        passwordEncoder.encode(password), "ADMIN");
                log.info("管理员 {} 的密码已初始化/轮换", username);
            } else {
                log.info("管理员 {} 已存在，保留页面修改后的密码", username);
            }
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
