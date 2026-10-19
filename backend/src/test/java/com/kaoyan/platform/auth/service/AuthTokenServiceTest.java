package com.kaoyan.platform.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kaoyan.platform.auth.entity.AuthToken;
import com.kaoyan.platform.auth.mapper.AuthTokenMapper;
import com.kaoyan.platform.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private AuthTokenMapper tokenMapper;

    private AuthTokenService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new AuthTokenService(tokenMapper, clock);
    }

    @Test
    @DisplayName("创建 token：数据库只存 hash，不存明文")
    void createStoresOnlyHash() {
        String raw = service.create(1L, AuthTokenService.RESET_PASSWORD, Duration.ofMinutes(30));

        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);
        verify(tokenMapper).insert(captor.capture());
        AuthToken saved = captor.getValue();
        assertEquals(64, saved.getTokenHash().length());
        assertNotEquals(raw, saved.getTokenHash());
        assertEquals(AuthTokenService.RESET_PASSWORD, saved.getType());
    }

    @Test
    @DisplayName("消费 token：未使用可消费，已使用则拒绝")
    void consumeIsOneTime() {
        String raw = service.create(1L, AuthTokenService.VERIFY_EMAIL, Duration.ofHours(1));
        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);
        verify(tokenMapper).insert(captor.capture());
        AuthToken saved = captor.getValue();
        saved.setExpiresAt(LocalDateTime.now(clock).plusHours(1));

        when(tokenMapper.selectByHash(anyString())).thenReturn(saved);
        when(tokenMapper.markUsed(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        assertEquals(1L, service.consume(raw, AuthTokenService.VERIFY_EMAIL));

        saved.setUsedAt(LocalDateTime.now(clock));
        assertThrows(BusinessException.class,
                () -> service.consume(raw, AuthTokenService.VERIFY_EMAIL));
    }
}
