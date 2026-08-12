package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import jakarta.validation.constraints.NotNull;

public record OrganizationalUnitStatusUpdateRequest(
        @NotNull(message = "Organizational unit status is required") OrganizationalUnitStatus status) {}
