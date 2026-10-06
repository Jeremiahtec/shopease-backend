package com.shopease.dto.payment;

import com.shopease.enums.OrderStatus;
import com.shopease.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String reference,
        Long orderId,
        BigDecimal amount,
        PaymentStatus status,
        OrderStatus orderStatus,
        LocalDateTime paidAt) {
}
