package com.kaoyan.platform.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

    private final AuthRateLimiter limiter = new AuthRateLimiter(
            Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("登录：5 次失败后第 6 次被限流，成功登录会清零")
    void loginLocksAfterFailures() {
        for (int i = 0; i < 5; i++) {
            limiter.onLoginFailure("alice", "127.0.0.1");
        }
        BusinessException e = assertThrows(BusinessException.class,
                () -> limiter.checkLogin("alice", "127.0.0.1"));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, e.getErrorCode());

        limiter.onLoginSuccess("alice", "127.0.0.1");
        assertDoesNotThrow(() -> limiter.checkLogin("alice", "127.0.0.1"));
    }

    @Test
    @DisplayName("登录：同一 IP 累计 30 次失败后，换用户名也会被限流")
    void loginLocksPerIpAcrossUsernames() {
        for (int i = 0; i < 30; i++) {
            limiter.onLoginFailure("user" + i, "10.0.0.9");
        }
        BusinessException e = assertThrows(BusinessException.class,
                () -> limiter.checkLogin("another_user", "10.0.0.9"));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, e.getErrorCode());
        assertDoesNotThrow(() -> limiter.checkLogin("another_user", "10.0.0.10"));
    }

    @Test
    @DisplayName("注册：同一 IP 每小时最多 10 次")
    void registerLocksPerIp() {
        for (int i = 0; i < 10; i++) {
            limiter.onRegister("10.0.0.1");
        }
        BusinessException e = assertThrows(BusinessException.class,
                () -> limiter.checkRegister("10.0.0.1"));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, e.getErrorCode());
        assertDoesNotThrow(() -> limiter.checkRegister("10.0.0.2"));
    }
}
