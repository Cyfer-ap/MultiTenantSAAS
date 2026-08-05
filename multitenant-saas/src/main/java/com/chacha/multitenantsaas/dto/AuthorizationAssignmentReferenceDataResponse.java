package com.chacha.multitenantsaas.dto;

import java.util.List;
import java.util.UUID;

public record AuthorizationAssignmentReferenceDataResponse(
        List<UserOption> users,
        List<ScopeTargetOption> organizationalUnits,
        List<ScopeTargetOption> projects,
        List<ScopeTargetOption> directReportsAnchors
) {

    public record UserOption(
            UUID id,
            String fullName,
            String email
    ) {
    }

    public record ScopeTargetOption(
            UUID id,
            String label,
            String description,
            UUID ownerUserId
    ) {
    }
}
