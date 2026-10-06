package com.shopease.dto.product;

import com.shopease.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @Schema(example = "Infinix Hot 40")
        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Name must be at most 200 characters")
        String name,

        @Schema(example = "6.78 inch display, 128GB storage")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Schema(example = "125000.00")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Price can have at most 2 decimal places")
        BigDecimal price,

        @Schema(example = "25")
        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Schema(example = "INF-HOT40-128")
        @NotBlank(message = "SKU is required")
        @Size(max = 64, message = "SKU must be at most 64 characters")
        String sku,

        @Schema(description = "Create a category first, then use its id", example = "1")
        @NotNull(message = "Category id is required")
        Long categoryId,

        @Schema(description = "Image URLs (upload to Cloudinary etc. and paste the links)")
        List<@NotBlank @Size(max = 500) String> imageUrls,

        @Schema(description = "ACTIVE (default) or INACTIVE (hidden from customers)")
        ProductStatus status) {
}
