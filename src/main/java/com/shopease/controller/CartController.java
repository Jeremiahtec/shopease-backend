package com.shopease.controller;

import com.shopease.dto.cart.CartItemRequest;
import com.shopease.dto.cart.CartResponse;
import com.shopease.dto.cart.UpdateCartItemRequest;
import com.shopease.security.AppUserDetails;
import com.shopease.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "6. Cart", description = "Logged-in customers use their account cart. Guests: send any unique X-Session-Id header.")
public class CartController {

    private static final String SESSION_HEADER = "X-Session-Id";

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "View my cart (customer token, or guest X-Session-Id)")
    public CartResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId) {
        return cartService.getCart(principal, sessionId);
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart (increases quantity if already there)")
    public CartResponse addItem(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
                                @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(principal, sessionId, request);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Set the quantity of a cart item")
    public CartResponse updateItem(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                   @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(principal, sessionId, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove an item from the cart")
    public CartResponse removeItem(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                   @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
                                   @PathVariable Long itemId) {
        return cartService.removeItem(principal, sessionId, itemId);
    }

    @PostMapping("/merge")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "After login: merge the guest cart (X-Session-Id) into my account cart")
    public CartResponse merge(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                              @RequestHeader(value = SESSION_HEADER) String sessionId) {
        return cartService.merge(principal, sessionId);
    }
}
