package com.shopease.dto.user;

import com.shopease.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean enabled,
        LocalDateTime createdAt) {
}
