package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;

import java.util.UUID;

public record OrganizationalUnitPathResponse(
        UUID id,
        UUID tenantId,
        UUID parentUnitId,
        String name,
        String code,
        OrganizationalUnitType type,
        OrganizationalUnitStatus status,
        int depth
) {
}