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
 * 只在 password_hash 为空时写入；生产用 DEMO_USER_ENABLED=false 关闭。
 */
@Slf4j
@Component
@Order(2)
public class DemoUserInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-user.enabled:true}")
    private boolean enabled;

    @Value("${app.demo-user.password:learner123}")
    private String password;

    public DemoUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        User user = userMapper.selectById(1L);
        if (user == null) {
            log.warn("demo user id=1 不存在，跳过演示密码初始化");
            return;
        }
        if (StringUtils.hasText(user.getPasswordHash())) {
            return;
        }
        userMapper.updatePasswordHash(1L, passwordEncoder.encode(password));
        log.info("demo user learner 的密码已初始化（仅首次）");
    }
}
