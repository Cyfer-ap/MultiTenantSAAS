package com.chacha.multitenantsaas.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentAuthorizationContextResponse(

        UUID tenantId,

        UUID userId,

        String fullName,

        String email,

        Instant evaluatedAt,

        List<String> tenantPermissionCodes,

        List<String> allPermissionCodes,

        List<CurrentAuthorizationGrantResponse> grants
) {
}