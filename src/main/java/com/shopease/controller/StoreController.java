package com.shopease.controller;

import com.shopease.dto.store.StoreRequest;
import com.shopease.dto.store.StoreResponse;
import com.shopease.security.AppUserDetails;
import com.shopease.service.StoreService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Tag(name = "3. Stores (Vendor)")
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor: create my store (one store per vendor)")
    public StoreResponse create(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                @Valid @RequestBody StoreRequest request) {
        return storeService.create(principal.getId(), request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor: get my store")
    public StoreResponse mine(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
        return storeService.getMine(principal.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Public: get a store by id")
    public StoreResponse get(@PathVariable Long id) {
        return storeService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor: update my store")
    public StoreResponse update(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                @PathVariable Long id,
                                @Valid @RequestBody StoreRequest request) {
        return storeService.update(principal.getId(), id, request);
    }
}
