package com.shopease.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record InitializePaymentRequest(
        @Schema(example = "1")
        @NotNull(message = "Order id is required")
        Long orderId) {
}
