package com.chacha.multitenantsaas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record AuthorizationRolePermissionUpdateRequest(

        @NotNull(
                message =
                        "Permission ids are required"
        )
        Set<@Valid @NotNull UUID> permissionIds
) {
}