package com.kaoyan.platform.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kaoyan.platform.user.entity.User;
import com.kaoyan.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminUserInitializer(userMapper, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "username", "ops_admin");
        ReflectionTestUtils.setField(initializer, "password", "OpsAdmin2026");
        ReflectionTestUtils.setField(initializer, "required", false);
    }

    @Test
    @DisplayName("管理员引导：不存在则创建 ADMIN")
    void createsAdmin() {
        when(userMapper.selectByUsername("ops_admin")).thenReturn(null);
        when(passwordEncoder.encode("OpsAdmin2026")).thenReturn("hash");

        initializer.run(null);

        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("管理员引导：已存在则更新密码并确保 ADMIN")
    void promotesExistingUser() {
        User existing = new User();
        existing.setId(9L);
        existing.setUsername("ops_admin");
        existing.setRole("USER");
        when(userMapper.selectByUsername("ops_admin")).thenReturn(existing);
        when(passwordEncoder.encode("OpsAdmin2026")).thenReturn("hash");

        initializer.run(null);

        verify(userMapper).updatePasswordAndRole(9L, "hash", "ADMIN");
    }

    @Test
    @DisplayName("生产要求管理员但未配置密码 → 启动失败")
    void requiredWithoutPasswordFails() {
        ReflectionTestUtils.setField(initializer, "password", "");
        ReflectionTestUtils.setField(initializer, "required", true);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> initializer.run(null));
        assertEquals(true, e.getMessage().contains("ADMIN_PASSWORD"));
    }
}
