package com.kaoyan.platform.report.cache;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ReportCacheServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;

    @Test
    @DisplayName("Redis 连接失败时缓存降级为 null，不抛异常")
    void redisFailureFallsBackToNull() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        ReportCacheService service = new ReportCacheService(redis, new ObjectMapper(), true, 60);

        assertNull(service.get(1L, LocalDate.of(2026, 10, 18)));
    }
}
