package com.shopease.dto.wishlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record WishlistRequest(
        @Schema(example = "1")
        @NotNull(message = "Product id is required")
        Long productId) {
}
