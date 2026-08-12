package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentAuthorizationGrantResponse(
        UUID assignmentId,
        UUID roleId,
        String roleCode,
        String roleName,
        AuthorizationRoleSource roleSource,
        AuthorizationScopeType scopeType,
        UUID scopeTargetId,
        Instant validFrom,
        Instant validUntil,
        List<String> permissionCodes) {}
