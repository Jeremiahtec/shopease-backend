package com.shopease.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Public address of the web app, used to build links in emails (password reset). */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(@DefaultValue("http://localhost:5173") String url) {
}
