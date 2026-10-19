package com.kaoyan.platform.auth.security;

import com.kaoyan.platform.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long expireSeconds;
    private final long refreshExpireSeconds;

    public JwtService(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || secret.contains("change-me") || secret.contains("dev-only")) {
            throw new IllegalStateException(
                    "JWT_SECRET 未配置或仍是示例值：请设置至少 32 字节的随机密钥");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = Duration.ofHours(Math.max(1, properties.getExpireHours())).toSeconds();
        this.refreshExpireSeconds = Duration.ofDays(Math.max(1, properties.getRefreshExpireDays())).toSeconds();
    }

    public String generate(Long userId, String username) {
        return generate(userId, username, 0);
    }

    public String generate(Long userId, String username, int tokenVersion) {
        return build(userId, username, tokenVersion, TYPE_ACCESS, expireSeconds);
    }

    public String generateRefresh(Long userId, String username, int tokenVersion) {
        return build(userId, username, tokenVersion, TYPE_REFRESH, refreshExpireSeconds);
    }

    public LoginUser parse(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public LoginUser parseRefresh(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private LoginUser parse(String token, String expectedType) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (!expectedType.equals(claims.get("type", String.class))) {
            throw new JwtException("token 类型不匹配");
        }
        Integer version = claims.get("ver", Integer.class);
        return new LoginUser(Long.valueOf(claims.getSubject()),
                claims.get("username", String.class), version == null ? 0 : version);
    }

    private String build(Long userId, String username, int tokenVersion,
                         String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("ver", tokenVersion)
                .claim("type", type)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }
}
