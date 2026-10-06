package com.shopease.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/** Browser origins allowed to call the API (comma separated in CORS_ALLOWED_ORIGINS). */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@DefaultValue("http://localhost:5173,http://localhost:3000") List<String> allowedOrigins) {
}
