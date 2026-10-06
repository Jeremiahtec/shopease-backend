package com.shopease.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Paystack paystack, Admin admin) {

    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays) {
    }

    public record Paystack(String secretKey, String baseUrl, String callbackUrl) {
        /** With no secret key configured the app uses a fake gateway so you can test the whole flow locally. */
        public boolean mockMode() {
            return secretKey == null || secretKey.isBlank();
        }
    }

    public record Admin(String email, String password, String fullName) {
    }
}
