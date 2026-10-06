package com.shopease.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Max requests per client IP per endpoint (login, register, password reset) within the window. */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(@DefaultValue("20") int maxRequests, @DefaultValue("60") int windowSeconds) {
}
