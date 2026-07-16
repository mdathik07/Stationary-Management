package com.example.demo.controller.dto;

import java.math.BigDecimal;

public record AdminDashboardStats(
        long pendingCount,
        long printingCount,
        long readyCount,
        long todayOrders,
        BigDecimal todayRevenue) {
}
