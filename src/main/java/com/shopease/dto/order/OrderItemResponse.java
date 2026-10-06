package com.shopease.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        Long storeId,
        String storeName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
