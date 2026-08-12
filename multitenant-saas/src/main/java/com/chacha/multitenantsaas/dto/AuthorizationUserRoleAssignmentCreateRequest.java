package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationUserRoleAssignmentCreateRequest(
        @NotNull(message = "User id is required") UUID userId,
        @NotNull(message = "Role id is required") UUID roleId,
        @NotNull(message = "Scope type is required") AuthorizationScopeType scopeType,
        UUID scopeTargetId,
        Instant validFrom,
        Instant validUntil) {}
