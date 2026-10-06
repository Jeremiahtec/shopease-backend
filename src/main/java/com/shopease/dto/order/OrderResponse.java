package com.shopease.dto.order;

import com.shopease.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        Long customerId,
        String customerName,
        List<OrderItemResponse> items,
        LocalDateTime createdAt) {
}
