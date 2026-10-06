package com.shopease.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin@shopease.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @Schema(example = "Admin@12345")
        @NotBlank(message = "Password is required")
        String password) {
}
