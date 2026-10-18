package com.kaoyan.platform.report.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.report.vo.DailyReportVO;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 报告 Cache-Aside：读先查 Redis，未命中查库回填；练习提交后删缓存。
 * Redis 不可用时全部降级为查库，不影响主流程。
 */
@Service
public class ReportCacheService {

    private static final Logger log = LoggerFactory.getLogger(ReportCacheService.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long ttlSeconds;
    /** Redis 故障后短暂熔断，避免每个请求都等连接超时 */
    private volatile long unavailableUntil = 0L;

    public ReportCacheService(StringRedisTemplate redis,
                              ObjectMapper objectMapper,
                              @Value("${app.cache.report.enabled:true}") boolean enabled,
                              @Value("${app.cache.report.ttl-seconds:90000}") long ttlSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
    }

    public DailyReportVO get(Long userId, LocalDate date) {
        if (!enabled || System.currentTimeMillis() < unavailableUntil) {
            return null;
        }
        try {
            String json = redis.opsForValue().get(key(userId, date));
            return json == null ? null : objectMapper.readValue(json, DailyReportVO.class);
        } catch (Exception e) {
            markUnavailable(e);
            return null;
        }
    }

    public void put(Long userId, LocalDate date, DailyReportVO report) {
        if (!enabled || System.currentTimeMillis() < unavailableUntil) {
            return;
        }
        try {
            // TTL 加随机抖动，避免同一批 key 同时过期造成雪崩
            long ttl = ttlSeconds + ThreadLocalRandom.current().nextLong(600);
            redis.opsForValue().set(key(userId, date),
                    objectMapper.writeValueAsString(report), Duration.ofSeconds(ttl));
        } catch (Exception e) {
            markUnavailable(e);
        }
    }

    public void evict(Long userId, LocalDate date) {
        if (!enabled || System.currentTimeMillis() < unavailableUntil) {
            return;
        }
        try {
            redis.delete(key(userId, date));
        } catch (Exception e) {
            markUnavailable(e);
        }
    }

    private void markUnavailable(Exception e) {
        unavailableUntil = System.currentTimeMillis() + 30_000L;
        log.warn("报告缓存不可用，30 秒内降级查库：{}", e.getMessage());
    }

    private String key(Long userId, LocalDate date) {
        return "report:" + userId + ":" + date;
    }
}
