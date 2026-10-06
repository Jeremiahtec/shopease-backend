package com.shopease.dto.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreRequest(
        @Schema(example = "Tech Hub Ogbomoso")
        @NotBlank(message = "Store name is required")
        @Size(max = 150, message = "Store name must be at most 150 characters")
        String name,

        @Schema(example = "Phones, laptops and accessories")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @Schema(example = "https://example.com/logo.png")
        @Size(max = 500, message = "Logo URL must be at most 500 characters")
        String logoUrl) {
}
