package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record OrganizationAssignmentCreateRequest(
        @NotNull(message = "User id is required") UUID userId,
        @NotNull(message = "Organizational unit id is required") UUID organizationalUnitId,
        UUID reportsToAssignmentId,
        @Size(max = 150, message = "Position title must not exceed " + "150 characters") String positionTitle,
        boolean primaryAssignment,
        Instant validFrom,
        Instant validUntil) {}
