package com.chacha.multitenantsaas.dto;

public record DashboardSummaryResponse(
        long totalTenants,
        long activeTenants,
        long inactiveTenants,
        long suspendedTenants,

        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long suspendedUsers
) {
}