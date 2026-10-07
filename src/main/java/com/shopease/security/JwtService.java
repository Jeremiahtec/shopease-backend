package com.shopease.security;

import com.shopease.config.AppProperties;
import com.shopease.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(AppProperties props, Environment environment) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "The JWT secret is missing or too short. Set the JWT_SECRET environment variable to at least 32 characters.");
        }
        if (environment.acceptsProfiles(Profiles.of("prod")) && secret.startsWith("change-me")) {
            throw new IllegalStateException("Set the JWT_SECRET environment variable before running with the prod profile");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTokenMinutes());
        this.refreshTtl = Duration.ofDays(props.jwt().refreshTokenDays());
    }

    public String generateAccessToken(User user) {
        return build(user, TYPE_ACCESS, accessTtl);
    }

    public String generateRefreshToken(User user) {
        return build(user, TYPE_REFRESH, refreshTtl);
    }

    /** Validates signature + expiry and returns the claims. Throws io.jsonwebtoken.JwtException when invalid. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private String build(User user, String type, Duration ttl) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("type", type)
                .claim("role", user.getRole().name())
                .claim("uid", user.getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }
}
