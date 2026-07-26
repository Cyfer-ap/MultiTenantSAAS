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
        long suspendedUsers,

        long totalProjects,
        long planningProjects,
        long activeProjects,
        long onHoldProjects,
        long completedProjects,
        long archivedProjects,

        long totalProjectMemberships,

        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long blockedTasks,
        long completedTasks,
        long cancelledTasks,
        long overdueTasks,
        double taskCompletionPercentage
) {
}