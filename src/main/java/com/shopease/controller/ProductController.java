package com.shopease.controller;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.product.ProductRequest;
import com.shopease.dto.product.ProductResponse;
import com.shopease.security.AppUserDetails;
import com.shopease.service.ProductService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "5. Products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Public: list products (paginated, sortable)",
            description = "sortBy: name | price | createdAt. direction: asc | desc")
    public PageResponse<ProductResponse> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String sortBy,
                                              @RequestParam(defaultValue = "desc") String direction) {
        return productService.search(null, null, null, null, null, page, size, sortBy, direction);
    }

    @GetMapping("/search")
    @Operation(summary = "Public: search by keyword, category, store and price range",
            description = "All filters are optional and can be combined.")
    public PageResponse<ProductResponse> search(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Long storeId,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String sortBy,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return productService.search(keyword, categoryId, storeId, minPrice, maxPrice, page, size, sortBy, direction);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor: list my products (including INACTIVE)")
    public PageResponse<ProductResponse> mine(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return productService.listMine(principal.getId(), page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Public: get a product by id")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor: create a product in my store")
    public ProductResponse create(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                  @Valid @RequestBody ProductRequest request) {
        return productService.create(principal.getId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor: update my product (price, stock, images...)")
    public ProductResponse update(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request) {
        return productService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor: delete (archive) my product")
    public void delete(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable Long id) {
        productService.delete(principal.getId(), id);
    }
}
