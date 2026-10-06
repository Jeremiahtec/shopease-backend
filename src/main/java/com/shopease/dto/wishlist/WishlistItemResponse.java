package com.shopease.dto.wishlist;

import com.shopease.dto.product.ProductResponse;

import java.time.LocalDateTime;

public record WishlistItemResponse(Long id, ProductResponse product, LocalDateTime addedAt) {
}
