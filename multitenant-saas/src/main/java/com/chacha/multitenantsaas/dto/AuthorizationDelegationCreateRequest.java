package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationDelegationCreateRequest(
        @NotNull(message = "Delegate user id is required") UUID delegateUserId,
        @NotNull(message = "Parent authority assignment id is required") UUID parentAssignmentId,
        @NotNull(message = "Role id is required") UUID roleId,
        @NotNull(message = "Scope type is required") AuthorizationScopeType scopeType,
        UUID scopeTargetId,
        Instant validFrom,
        @NotNull(message = "Delegation valid-until time is required") Instant validUntil) {}
