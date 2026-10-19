package com.kaoyan.platform.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kaoyan.platform.auth.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService service() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-0123456789abcdef0123456789abcdef");
        properties.setExpireHours(1);
        return new JwtService(properties);
    }

    @Test
    @DisplayName("JWT：签发后可解析出用户，非法 token 被拒绝")
    void generateAndParse() {
        JwtService jwt = service();
        String token = jwt.generate(42L, "alice");

        LoginUser user = jwt.parse(token);

        assertEquals(42L, user.id());
        assertEquals("alice", user.username());
        assertThrows(JwtException.class, () -> jwt.parse("not-a-jwt"));
    }

    @Test
    @DisplayName("JWT：access/refresh 类型不可混用")
    void tokenTypesAreSeparated() {
        JwtService jwt = service();
        String refresh = jwt.generateRefresh(42L, "alice", 3);
        LoginUser user = jwt.parseRefresh(refresh);
        assertEquals(3, user.tokenVersion());
        assertThrows(JwtException.class, () -> jwt.parse(refresh));
    }

    @Test
    @DisplayName("默认/示例 JWT secret 必须被拒绝")
    void rejectsDefaultSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("dev-only-jwt-secret-change-me-0123456789abcdef0123456789abcdef");
        assertThrows(IllegalStateException.class, () -> new JwtService(properties));
    }
}
