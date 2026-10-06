package com.shopease.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
        @Schema(example = "12 Ilorin Road, Ogbomoso, Oyo State")
        @NotBlank(message = "Shipping address is required")
        @Size(max = 500, message = "Shipping address must be at most 500 characters")
        String shippingAddress) {
}
