package com.shopease.config;

import org.springframework.core.env.Environment;

/**
 * Core settings (JWT, Paystack, admin account). Built straight from the Spring Environment, see
 * {@link AppPropertiesConfig}, so a missing or misnamed setting shows up as a clear message at startup.
 */
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

    public static AppProperties from(Environment env) {
        return new AppProperties(
                new Jwt(
                        env.getProperty("app.jwt.secret", ""),
                        env.getProperty("app.jwt.access-token-minutes", Long.class, 60L),
                        env.getProperty("app.jwt.refresh-token-days", Long.class, 7L)),
                new Paystack(
                        env.getProperty("app.paystack.secret-key", ""),
                        env.getProperty("app.paystack.base-url", "https://api.paystack.co"),
                        env.getProperty("app.paystack.callback-url", "http://localhost:5173/payment/callback")),
                new Admin(
                        env.getProperty("app.admin.email", "admin@shopease.com"),
                        env.getProperty("app.admin.password", "Admin@12345"),
                        env.getProperty("app.admin.full-name", "ShopEase Admin")));
    }
}
