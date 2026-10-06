package com.shopease.controller;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.wishlist.WishlistItemResponse;
import com.shopease.dto.wishlist.WishlistRequest;
import com.shopease.security.AppUserDetails;
import com.shopease.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "10. Wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    @Operation(summary = "Add a product to my wishlist")
    public WishlistItemResponse add(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                    @Valid @RequestBody WishlistRequest request) {
        return wishlistService.add(principal.getId(), request.productId());
    }

    @GetMapping
    @Operation(summary = "List my wishlist")
    public PageResponse<WishlistItemResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return wishlistService.list(principal.getId(), page, size);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a product from my wishlist")
    public void remove(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable Long productId) {
        wishlistService.remove(principal.getId(), productId);
    }
}
