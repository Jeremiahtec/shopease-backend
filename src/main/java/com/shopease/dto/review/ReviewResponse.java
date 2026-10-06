package com.shopease.dto.review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String reviewerName,
        int rating,
        String comment,
        LocalDateTime createdAt) {
}
