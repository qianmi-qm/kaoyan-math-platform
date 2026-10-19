package com.kaoyan.platform.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kaoyan.platform.ai.entity.AiUsage;
import com.kaoyan.platform.ai.mapper.AiUsageMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @Mock
    private AiUsageMapper usageMapper;

    private AiQuotaService service;
    private final LocalDate today = LocalDate.of(2026, 10, 19);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);
        service = new AiQuotaService(usageMapper, clock, 20);
    }

    @Test
    @DisplayName("配额扣减：达到上限返回 false")
    void consumeRespectsLimit() {
        when(usageMapper.tryConsume(1L, today, 20)).thenReturn(1);
        assertTrue(service.tryConsume(1L));

        when(usageMapper.tryConsume(1L, today, 20)).thenReturn(0);
        assertFalse(service.tryConsume(1L));
    }

    @Test
    @DisplayName("配额查询：已用/剩余正确")
    void quotaReportsRemaining() {
        AiUsage usage = new AiUsage();
        usage.setRequestCount(7);
        when(usageMapper.selectByUserAndDate(1L, today)).thenReturn(usage);

        var quota = service.quota(1L);
        assertEquals(7, quota.getUsed());
        assertEquals(13, quota.getRemaining());

        service.addTokens(1L, 100);
        verify(usageMapper).addTokens(1L, today, 100);
    }
}
