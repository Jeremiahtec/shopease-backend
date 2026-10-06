package com.shopease.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Unpaid orders hold stock; after this many minutes they are cancelled and the stock is released. */
@ConfigurationProperties(prefix = "app.orders")
public record OrderProperties(@DefaultValue("120") int pendingExpiryMinutes) {
}
