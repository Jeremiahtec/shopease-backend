package com.shopease.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPropertiesTest {

    @Test
    void readsConfiguredValues() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("app.jwt.secret", "a-secret-that-is-long-enough-for-hs256-signing")
                .withProperty("app.jwt.access-token-minutes", "15")
                .withProperty("app.paystack.secret-key", "sk_test_123")
                .withProperty("app.admin.email", "boss@shop.com");

        AppProperties props = AppProperties.from(env);

        assertEquals("a-secret-that-is-long-enough-for-hs256-signing", props.jwt().secret());
        assertEquals(15, props.jwt().accessTokenMinutes());
        assertEquals(7, props.jwt().refreshTokenDays());           // default
        assertFalse(props.paystack().mockMode());
        assertEquals("boss@shop.com", props.admin().email());
    }

    @Test
    void usesSafeDefaultsAndNeverReturnsNullGroups() {
        AppProperties props = AppProperties.from(new MockEnvironment());

        assertEquals("", props.jwt().secret());                    // JwtService then refuses to start with a clear message
        assertTrue(props.paystack().mockMode());
        assertEquals("https://api.paystack.co", props.paystack().baseUrl());
    }
}
