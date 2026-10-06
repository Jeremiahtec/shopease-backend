package com.shopease.controller;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.review.ReviewRequest;
import com.shopease.dto.review.ReviewResponse;
import com.shopease.security.AppUserDetails;
import com.shopease.service.ReviewService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "9. Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Customer: review a product I have bought (one review per product)")
    public ReviewResponse add(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                              @PathVariable Long productId,
                              @Valid @RequestBody ReviewRequest request) {
        return reviewService.add(principal.getId(), productId, request);
    }

    @GetMapping
    @Operation(summary = "Public: list reviews for a product")
    public PageResponse<ReviewResponse> list(@PathVariable Long productId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return reviewService.list(productId, page, size);
    }
}
