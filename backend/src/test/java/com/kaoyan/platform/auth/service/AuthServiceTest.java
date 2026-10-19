package com.kaoyan.platform.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.kaoyan.platform.auth.dto.AuthResponse;
import com.kaoyan.platform.auth.dto.LoginRequest;
import com.kaoyan.platform.auth.dto.RegisterRequest;
import com.kaoyan.platform.auth.security.JwtService;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.user.entity.User;
import com.kaoyan.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuthRateLimiter rateLimiter;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userMapper, passwordEncoder, jwtService, currentUser, rateLimiter);
    }

    @Test
    @DisplayName("注册：BCrypt 后入库，返回 token")
    void registerCreatesUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret1");
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(passwordEncoder.encode("secret1")).thenReturn("bcrypt-hash");
        doAnswer(invocation -> {
            User created = invocation.getArgument(0);
            created.setId(2L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(jwtService.generate(2L, "alice", 0)).thenReturn("jwt-token");
        when(jwtService.generateRefresh(2L, "alice", 0)).thenReturn("refresh-token");
        when(jwtService.getExpireSeconds()).thenReturn(3600L);

        AuthResponse response = service.register(request, "127.0.0.1");

        assertEquals("jwt-token", response.getToken());
        assertEquals(2L, response.getUserId());
        assertEquals("alice", response.getUsername());
        assertEquals("USER", response.getRole());
    }

    @Test
    @DisplayName("登录：密码错误返回 401，不泄露用户是否存在")
    void loginRejectsWrongPassword() {
        User user = new User();
        user.setId(2L);
        user.setUsername("alice");
        user.setPasswordHash("bcrypt-hash");
        when(userMapper.selectByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("bad-pass", "bcrypt-hash")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("bad-pass");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.login(request, "127.0.0.1"));
        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
    }
}
