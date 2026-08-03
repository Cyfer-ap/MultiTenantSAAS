package com.chacha.multitenantsaas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record AuthorizationRoleCreateRequest(

        @NotBlank(message = "Role code is required")
        @Size(
                max = 60,
                message =
                        "Role code must not exceed "
                                + "60 characters"
        )
        String code,

        @NotBlank(message = "Role name is required")
        @Size(
                max = 150,
                message =
                        "Role name must not exceed "
                                + "150 characters"
        )
        String name,

        @Size(
                max = 500,
                message =
                        "Role description must not exceed "
                                + "500 characters"
        )
        String description,

        @NotNull(
                message =
                        "Permission ids are required"
        )
        Set<@Valid @NotNull UUID> permissionIds
) {
}