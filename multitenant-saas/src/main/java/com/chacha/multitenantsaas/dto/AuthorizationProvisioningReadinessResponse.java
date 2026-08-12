package com.chacha.multitenantsaas.dto;

import java.util.List;
import java.util.UUID;

public record AuthorizationProvisioningReadinessResponse(
        UUID tenantId,
        boolean ready,
        int activeSystemRolesAvailable,
        List<String> missingSystemRoleCodes,
        int usersScanned,
        int activeUsers,
        int inactiveUsers,
        int compliantUsers,
        int unresolvedUsers,
        List<AuthorizationProvisioningIssueResponse> issues) {}
