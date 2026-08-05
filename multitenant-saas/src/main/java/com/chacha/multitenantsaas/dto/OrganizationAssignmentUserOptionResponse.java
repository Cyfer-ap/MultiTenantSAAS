package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record OrganizationAssignmentUserOptionResponse(
        UUID id,
        String fullName,
        String email
) {
}
