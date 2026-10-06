package com.shopease.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @Schema(example = "3")
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1 (use DELETE to remove an item)")
        Integer quantity) {
}
