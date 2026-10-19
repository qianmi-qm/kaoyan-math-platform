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
 * 本地演示账号：user_id=1 / learner。
 * 默认关闭；开启时必须显式提供非默认密码，关闭时会清掉历史 demo 密码。
 */
@Slf4j
@Component
@Order(2)
public class DemoUserInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-user.enabled:false}")
    private boolean enabled;

    @Value("${app.demo-user.password:}")
    private String password;

    public DemoUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User user = userMapper.selectById(1L);
        if (user == null || !"learner".equals(user.getUsername())) {
            log.warn("demo user learner（id=1）不存在，跳过 demo 账号处理");
            return;
        }
        if (!enabled) {
            int cleared = userMapper.clearDemoUser(1L);
            if (cleared > 0) {
                log.info("demo user 已关闭，learner 的历史密码已清除");
            }
            return;
        }
        if (!StringUtils.hasText(password) || "learner123".equals(password)
                || password.contains("change-me")) {
            throw new IllegalStateException(
                    "DEMO_USER_ENABLED=true 时必须设置 DEMO_USER_PASSWORD，且不能使用默认密码");
        }
        if (!StringUtils.hasText(user.getPasswordHash())) {
            userMapper.updatePasswordHash(1L, passwordEncoder.encode(password));
            log.info("demo user learner 密码已初始化（ADMIN）");
        } else {
            log.info("demo user learner 已存在，保留页面修改后的密码");
        }
        userMapper.updateRole(1L, "ADMIN");
    }
}
