package com.shopease.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String storeName,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int availableStock) {
}
