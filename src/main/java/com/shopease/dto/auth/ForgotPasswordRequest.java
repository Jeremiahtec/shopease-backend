package com.shopease.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @Schema(example = "ada@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email) {
}
