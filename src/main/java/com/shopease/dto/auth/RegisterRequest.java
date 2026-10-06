package com.shopease.dto.auth;

import com.shopease.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Ada Okafor")
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @Schema(example = "ada@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @Schema(example = "Password@123")
        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "Password must be 8-72 characters and include a letter and a number")
        String password,

        @Schema(example = "08012345678")
        @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone number is not valid")
        String phone,

        @Schema(description = "CUSTOMER (default) or VENDOR. ADMIN cannot self-register.", example = "CUSTOMER")
        Role role) {
}
