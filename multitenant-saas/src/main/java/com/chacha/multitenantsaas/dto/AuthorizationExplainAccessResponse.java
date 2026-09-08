package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.security.AuthorizationAccessDecisionReason;
import java.time.Instant;
import java.util.UUID;

public record AuthorizationExplainAccessResponse(
        UUID tenantId,
        UUID userId,
        String permissionCode,
        AuthorizationAccessContextType contextType,
        UUID targetId,
        boolean granted,
        AuthorizationAccessDecisionReason reason,
        Instant evaluatedAt,
        AuthorizationMatchedGrantResponse matchedGrant) {}
