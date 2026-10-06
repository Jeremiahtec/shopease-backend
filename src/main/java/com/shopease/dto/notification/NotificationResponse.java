package com.shopease.dto.notification;

import com.shopease.enums.OrderStatus;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        Long orderId,
        OrderStatus status,
        boolean seen,
        LocalDateTime createdAt) {
}
