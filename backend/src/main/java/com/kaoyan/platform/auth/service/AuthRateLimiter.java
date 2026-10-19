package com.kaoyan.platform.auth.service;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录/注册限流。优先用 Redis 计数器，多实例共享；Redis 不可用时降级到本地内存。
 * 登录：username+IP 5 次/5 分钟，IP 30 次/5 分钟；注册：IP 10 次/小时。
 */
@Component
public class AuthRateLimiter {

    static final int LOGIN_MAX_FAILURES = 5;
    static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);
    static final int LOGIN_IP_MAX_FAILURES = 30;
    static final int REGISTER_MAX = 10;
    static final Duration REGISTER_WINDOW = Duration.ofHours(1);
    private static final long CLEANUP_EVERY = 256;
    private static final long REDIS_RETRY_MS = 30_000L;
    private static final int MAX_LOCAL_COUNTERS = 20_000;

    private final Clock clock;
    private final StringRedisTemplate redis;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private volatile long redisDownUntil = 0L;

    public AuthRateLimiter(Clock clock) {
        this(clock, (StringRedisTemplate) null);
    }

    @Autowired
    public AuthRateLimiter(Clock clock, ObjectProvider<StringRedisTemplate> redisProvider) {
        this(clock, redisProvider.getIfAvailable());
    }

    private AuthRateLimiter(Clock clock, StringRedisTemplate redis) {
        this.clock = clock;
        this.redis = redis;
    }

    public void checkLogin(String username, String ip) {
        check("login:" + username.toLowerCase() + ":" + ip, LOGIN_MAX_FAILURES, LOGIN_WINDOW);
        check("login-ip:" + ip, LOGIN_IP_MAX_FAILURES, LOGIN_WINDOW);
    }

    public void onLoginFailure(String username, String ip) {
        increment("login:" + username.toLowerCase() + ":" + ip, LOGIN_WINDOW);
        increment("login-ip:" + ip, LOGIN_WINDOW);
    }

    public void onLoginSuccess(String username, String ip) {
        String key = "login:" + username.toLowerCase() + ":" + ip;
        deleteRedis(key);
        counters.remove(key);
    }

    public void checkRegister(String ip) {
        check("register:" + ip, REGISTER_MAX, REGISTER_WINDOW);
    }

    public void onRegister(String ip) {
        increment("register:" + ip, REGISTER_WINDOW);
    }

    private void check(String key, int max, Duration window) {
        if (checkRedis(key, max, window)) {
            return;
        }
        checkLocal(key, max, window);
    }

    private void increment(String key, Duration window) {
        if (incrementRedis(key, window)) {
            return;
        }
        incrementLocal(key, window);
    }

    private boolean checkRedis(String key, int max, Duration window) {
        if (redis == null || System.currentTimeMillis() < redisDownUntil) {
            return false;
        }
        try {
            String raw = redis.opsForValue().get(key);
            int count = raw == null ? 0 : Integer.parseInt(raw);
            if (count >= max) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                        "请求过于频繁，请 " + window.toMinutes() + " 分钟后再试");
            }
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            markRedisDown();
            return false;
        }
    }

    private boolean incrementRedis(String key, Duration window) {
        if (redis == null || System.currentTimeMillis() < redisDownUntil) {
            return false;
        }
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, window);
            }
            return true;
        } catch (Exception e) {
            markRedisDown();
            return false;
        }
    }

    private void deleteRedis(String key) {
        if (redis == null || System.currentTimeMillis() < redisDownUntil) {
            return;
        }
        try {
            redis.delete(key);
        } catch (Exception e) {
            markRedisDown();
        }
    }

    private void checkLocal(String key, int max, Duration window) {
        cleanupIfNeeded();
        Counter counter = counters.computeIfAbsent(key, ignored -> new Counter(clock.instant()));
        synchronized (counter) {
            resetIfExpired(counter, window);
            if (counter.count >= max) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                        "请求过于频繁，请 " + window.toMinutes() + " 分钟后再试");
            }
        }
    }

    private void incrementLocal(String key, Duration window) {
        cleanupIfNeeded();
        Counter counter = counters.computeIfAbsent(key, ignored -> new Counter(clock.instant()));
        synchronized (counter) {
            resetIfExpired(counter, window);
            counter.count++;
        }
    }

    private void cleanupIfNeeded() {
        if (operations.incrementAndGet() % CLEANUP_EVERY != 0) {
            return;
        }
        Instant now = clock.instant();
        counters.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().start, now).compareTo(REGISTER_WINDOW) >= 0);
        if (counters.size() > MAX_LOCAL_COUNTERS) {
            counters.clear();
        }
    }

    private void resetIfExpired(Counter counter, Duration window) {
        Instant now = clock.instant();
        if (Duration.between(counter.start, now).compareTo(window) >= 0) {
            counter.start = now;
            counter.count = 0;
        }
    }

    private void markRedisDown() {
        redisDownUntil = System.currentTimeMillis() + REDIS_RETRY_MS;
    }

    private static final class Counter {
        private Instant start;
        private int count;

        private Counter(Instant start) {
            this.start = start;
        }
    }
}
