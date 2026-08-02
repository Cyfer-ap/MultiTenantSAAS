package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;

import java.time.Instant;
import java.util.UUID;

public record OrganizationalUnitResponse(
        UUID id,
        UUID tenantId,
        UUID parentUnitId,
        String name,
        String code,
        OrganizationalUnitType type,
        OrganizationalUnitStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}