package com.shopease.dto.payment;

import java.math.BigDecimal;

public record PaymentInitResponse(
        String reference,
        String authorizationUrl,
        String accessCode,
        BigDecimal amount,
        boolean mock) {
}
