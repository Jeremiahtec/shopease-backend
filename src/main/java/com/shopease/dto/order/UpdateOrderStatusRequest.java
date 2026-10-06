package com.shopease.dto.order;

import com.shopease.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @Schema(description = "Vendors: PROCESSING, SHIPPED, CANCELLED. Customers: CANCELLED (unpaid orders) or DELIVERED (confirm that a shipped order arrived).",
                example = "PROCESSING")
        @NotNull(message = "Status is required")
        OrderStatus status) {
}
