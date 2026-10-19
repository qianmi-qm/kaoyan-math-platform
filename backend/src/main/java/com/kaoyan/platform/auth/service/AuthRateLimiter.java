package com.kaoyan.platform.auth.service;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 单实例内存限流：登录按 username+IP 失败计数，注册按 IP 计数。
 * 多实例部署时应换成 Redis 计数器；这里先堵住单机暴力破解和批量注册。
 */
@Component
public class AuthRateLimiter {

    static final int LOGIN_MAX_FAILURES = 5;
    static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);
    static final int REGISTER_MAX = 10;
    static final Duration REGISTER_WINDOW = Duration.ofHours(1);

    private final Clock clock;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public AuthRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkLogin(String username, String ip) {
        check("login:" + username.toLowerCase() + ":" + ip, LOGIN_MAX_FAILURES, LOGIN_WINDOW);
    }

    public void onLoginFailure(String username, String ip) {
        increment("login:" + username.toLowerCase() + ":" + ip, LOGIN_WINDOW);
    }

    public void onLoginSuccess(String username, String ip) {
        counters.remove("login:" + username.toLowerCase() + ":" + ip);
    }

    public void checkRegister(String ip) {
        check("register:" + ip, REGISTER_MAX, REGISTER_WINDOW);
    }

    public void onRegister(String ip) {
        increment("register:" + ip, REGISTER_WINDOW);
    }

    private void check(String key, int max, Duration window) {
        Counter counter = counters.computeIfAbsent(key, ignored -> new Counter(clock.instant()));
        synchronized (counter) {
            resetIfExpired(counter, window);
            if (counter.count >= max) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                        "请求过于频繁，请 " + window.toMinutes() + " 分钟后再试");
            }
        }
    }

    private void increment(String key, Duration window) {
        Counter counter = counters.computeIfAbsent(key, ignored -> new Counter(clock.instant()));
        synchronized (counter) {
            resetIfExpired(counter, window);
            counter.count++;
        }
    }

    private void resetIfExpired(Counter counter, Duration window) {
        Instant now = clock.instant();
        if (Duration.between(counter.start, now).compareTo(window) >= 0) {
            counter.start = now;
            counter.count = 0;
        }
    }

    private static final class Counter {
        private Instant start;
        private int count;

        private Counter(Instant start) {
            this.start = start;
        }
    }
}
