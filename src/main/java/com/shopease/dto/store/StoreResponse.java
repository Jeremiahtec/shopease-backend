package com.shopease.dto.store;

import java.time.LocalDateTime;

public record StoreResponse(
        Long id,
        String name,
        String description,
        String logoUrl,
        boolean active,
        Long ownerId,
        LocalDateTime createdAt) {
}
