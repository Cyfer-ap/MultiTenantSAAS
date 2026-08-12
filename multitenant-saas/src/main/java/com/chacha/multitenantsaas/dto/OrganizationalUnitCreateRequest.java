package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OrganizationalUnitCreateRequest(
        UUID parentUnitId,
        @NotBlank(message = "Organizational unit name is required") @Size(
                        max = 150,
                        message = "Organizational unit name must not " + "exceed 150 characters")
                String name,
        @Size(max = 100, message = "Organizational unit code must not " + "exceed 100 characters") String code,
        @NotNull(message = "Organizational unit type is required") OrganizationalUnitType type) {}
