package com.kaoyan.platform.auth.security;

import com.kaoyan.platform.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
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

    private final SecretKey key;
    private final long expireSeconds;

    public JwtService(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || secret.contains("change-me") || secret.contains("dev-only")) {
            throw new IllegalStateException(
                    "JWT_SECRET 未配置或仍是示例值：请设置至少 32 字节的随机密钥");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = Duration.ofHours(Math.max(1, properties.getExpireHours())).toSeconds();
    }

    public String generate(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public LoginUser parse(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return new LoginUser(Long.valueOf(claims.getSubject()),
                claims.get("username", String.class));
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
