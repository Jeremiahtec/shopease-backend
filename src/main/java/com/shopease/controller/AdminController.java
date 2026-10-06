package com.shopease.controller;

import com.shopease.dto.admin.DashboardResponse;
import com.shopease.dto.common.MessageResponse;
import com.shopease.dto.common.PageResponse;
import com.shopease.dto.order.OrderResponse;
import com.shopease.dto.store.StoreResponse;
import com.shopease.dto.user.UserResponse;
import com.shopease.enums.Role;
import com.shopease.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "11. Admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(summary = "Platform totals: users, stores, products, orders, revenue")
    public DashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (optionally filter by role)")
    public PageResponse<UserResponse> users(@RequestParam(required = false) Role role,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return adminService.listUsers(role, page, size);
    }

    @GetMapping("/stores")
    @Operation(summary = "List all stores")
    public PageResponse<StoreResponse> stores(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return adminService.listStores(page, size);
    }

    @GetMapping("/orders")
    @Operation(summary = "List all orders")
    public PageResponse<OrderResponse> orders(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return adminService.listOrders(page, size);
    }

    @PutMapping("/vendors/{id}/suspend")
    @Operation(summary = "Suspend a vendor (blocks login, hides their store and products)")
    public MessageResponse suspend(@PathVariable Long id) {
        return adminService.suspendVendor(id);
    }

    @PutMapping("/vendors/{id}/activate")
    @Operation(summary = "Re-activate a suspended vendor")
    public MessageResponse activate(@PathVariable Long id) {
        return adminService.activateVendor(id);
    }
}
