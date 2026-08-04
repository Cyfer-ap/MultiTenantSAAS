package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;

import java.util.List;
import java.util.UUID;

public record AuthorizationProvisioningIssueResponse(

        UUID userId,

        String email,

        UserRole legacyRole,

        UserStatus userStatus,

        String expectedSystemRoleCode,

        List<String> activeSystemRoleCodes,

        String reason
) {
}