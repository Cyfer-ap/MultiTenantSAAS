package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;

import java.util.List;
import java.util.UUID;

public record OrganizationalUnitTreeResponse(
        UUID id,
        UUID tenantId,
        UUID parentUnitId,
        String name,
        String code,
        OrganizationalUnitType type,
        OrganizationalUnitStatus status,
        List<OrganizationalUnitTreeResponse> children
) {
}