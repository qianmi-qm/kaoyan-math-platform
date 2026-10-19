package com.kaoyan.platform.auth.service;

import com.kaoyan.platform.auth.entity.AuthToken;
import com.kaoyan.platform.auth.mapper.AuthTokenMapper;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;

/** 一次性认证 token：只在邮件里放明文，数据库只存 SHA-256。 */
@Service
public class AuthTokenService {

    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String VERIFY_EMAIL = "VERIFY_EMAIL";

    private final AuthTokenMapper tokenMapper;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthTokenService(AuthTokenMapper tokenMapper, Clock clock) {
        this.tokenMapper = tokenMapper;
        this.clock = clock;
    }

    public String create(Long userId, String type, Duration ttl) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthToken token = new AuthToken();
        token.setUserId(userId);
        token.setType(type);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(LocalDateTime.now(clock).plus(ttl));
        tokenMapper.insert(token);
        return raw;
    }

    public Long consume(String raw, String expectedType) {
        AuthToken token = tokenMapper.selectByHash(sha256(raw));
        if (token == null || !expectedType.equals(token.getType())
                || token.getUsedAt() != null
                || token.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "链接无效或已过期");
        }
        int updated = tokenMapper.markUsed(token.getTokenHash(), LocalDateTime.now(clock));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "链接已被使用");
        }
        return token.getUserId();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
