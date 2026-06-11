package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantStatus;

import java.util.UUID;

public record TenantDashboardSummaryResponse(
        UUID tenantId,
        String tenantName,
        String tenantSlug,
        TenantStatus tenantStatus,

        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long suspendedUsers
) {
}