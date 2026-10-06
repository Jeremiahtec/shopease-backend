package com.shopease.security;

import com.shopease.config.AppProperties;
import com.shopease.entity.User;
import com.shopease.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("unit-test-secret-that-is-definitely-longer-than-32-bytes", 60, 7),
                new AppProperties.Paystack("", "https://api.paystack.co", "http://localhost/cb"),
                new AppProperties.Admin("admin@test.com", "pw", "Admin"));
        jwtService = new JwtService(props, new MockEnvironment());
        user = new User();
        user.setId(42L);
        user.setEmail("ada@example.com");
        user.setRole(Role.VENDOR);
    }

    @Test
    void accessTokenCarriesSubjectRoleAndType() {
        Claims claims = jwtService.parse(jwtService.generateAccessToken(user));
        assertEquals("ada@example.com", claims.getSubject());
        assertEquals("VENDOR", claims.get("role", String.class));
        assertEquals(JwtService.TYPE_ACCESS, claims.get("type", String.class));
    }

    @Test
    void refreshTokenIsMarkedAsRefresh() {
        Claims claims = jwtService.parse(jwtService.generateRefreshToken(user));
        assertEquals(JwtService.TYPE_REFRESH, claims.get("type", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }

    @Test
    void shortSecretIsRejected() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("too-short", 60, 7),
                new AppProperties.Paystack("", "https://api.paystack.co", "http://localhost/cb"),
                new AppProperties.Admin("a@b.c", "pw", "A"));
        assertThrows(IllegalStateException.class, () -> new JwtService(props, new MockEnvironment()));
    }
}
