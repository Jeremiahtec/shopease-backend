package com.shopease.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopease.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    private final RateLimitFilter filter =
            new RateLimitFilter(new RateLimitProperties(3, 60), new ObjectMapper().findAndRegisterModules());

    private MockHttpServletResponse post(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksTheFourthLoginAttemptFromTheSameIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, post("/api/auth/login", "10.0.0.1").getStatus());
        }
        MockHttpServletResponse blocked = post("/api/auth/login", "10.0.0.1");
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertTrue(blocked.getContentAsString().contains("Too many attempts"));
    }

    @Test
    void otherIpsAndOtherEndpointsAreNotAffected() throws Exception {
        for (int i = 0; i < 4; i++) {
            post("/api/auth/login", "10.0.0.2");
        }
        assertEquals(200, post("/api/auth/login", "10.0.0.3").getStatus());
        assertEquals(200, post("/api/auth/register", "10.0.0.2").getStatus());
    }

    @Test
    void ordinaryEndpointsAreNeverLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, post("/api/products", "10.0.0.4").getStatus());
        }
    }
}
