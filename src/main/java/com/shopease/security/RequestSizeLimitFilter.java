package com.shopease.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopease.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/** Rejects absurdly large request bodies (every JSON payload in this API is a few kilobytes at most). */
@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final long MAX_BYTES = 1_048_576L; // 1 MB

    private final ObjectMapper objectMapper;

    public RequestSizeLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ApiError(LocalDateTime.now(), 413,
                    "Payload Too Large", "The request body is too large", request.getRequestURI(), null));
            return;
        }
        chain.doFilter(request, response);
    }
}
