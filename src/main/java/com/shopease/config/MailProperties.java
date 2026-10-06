package com.shopease.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Resend (https://resend.com) settings. With no API key, emails are only written to the log. */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        @DefaultValue("") String resendApiKey,
        @DefaultValue("ShopEase <onboarding@resend.dev>") String from) {

    public boolean enabled() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }
}
