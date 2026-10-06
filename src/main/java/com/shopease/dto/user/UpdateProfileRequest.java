package com.shopease.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Schema(example = "Ada Okafor")
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @Schema(example = "08012345678")
        @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone number is not valid")
        String phone) {
}
