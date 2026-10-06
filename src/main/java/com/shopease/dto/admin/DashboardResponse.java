package com.shopease.dto.admin;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalUsers,
        long totalCustomers,
        long totalVendors,
        long totalStores,
        long totalProducts,
        long totalOrders,
        BigDecimal totalRevenue) {
}
