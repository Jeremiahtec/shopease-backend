package com.shopease.controller;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.order.OrderResponse;
import com.shopease.dto.order.PlaceOrderRequest;
import com.shopease.dto.order.UpdateOrderStatusRequest;
import com.shopease.security.AppUserDetails;
import com.shopease.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "7. Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Customer: checkout my cart into a PENDING order")
    public OrderResponse place(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                               @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(principal.getId(), request);
    }

    @GetMapping
    @Operation(summary = "List orders (customer: mine, vendor: orders with my products, admin: all)")
    public PageResponse<OrderResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return orderService.list(principal, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one order")
    public OrderResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                             @PathVariable Long id) {
        return orderService.get(principal, id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUSTOMER','VENDOR')")
    @Operation(summary = "Update order status",
            description = "Vendor: PROCESSING -> SHIPPED (or CANCELLED). Customer: confirms arrival (DELIVERED) once shipped, or cancels while PENDING.")
    public OrderResponse updateStatus(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(principal, id, request.status());
    }
}
