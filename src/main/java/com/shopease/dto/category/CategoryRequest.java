package com.shopease.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @Schema(example = "Electronics")
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must be at most 100 characters")
        String name,

        @Schema(example = "Phones, laptops and gadgets")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description) {
}
