package com.shopease.dto.product;

import com.shopease.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        String sku,
        ProductStatus status,
        Long categoryId,
        String categoryName,
        Long storeId,
        String storeName,
        List<String> imageUrls,
        LocalDateTime createdAt) {
}
