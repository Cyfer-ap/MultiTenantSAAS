package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.security.AuthorizationGrantSource;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationMatchedGrantResponse(
        UUID assignmentId,
        UUID roleId,
        String roleCode,
        AuthorizationScopeType scopeType,
        UUID scopeTargetId,
        Instant validFrom,
        Instant validUntil,
        AuthorizationGrantSource grantSource,
        UUID delegationId,
        UUID parentAssignmentId,
        UUID delegatorUserId,
        String delegatorEmail) {}
