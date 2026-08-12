package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import java.time.Instant;
import java.util.UUID;

public record OrganizationAssignmentResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userFullName,
        UUID organizationalUnitId,
        String organizationalUnitName,
        UUID reportsToAssignmentId,
        UUID managerUserId,
        String managerUserFullName,
        String positionTitle,
        boolean primaryAssignment,
        OrganizationAssignmentStatus status,
        Instant validFrom,
        Instant validUntil,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt) {}
