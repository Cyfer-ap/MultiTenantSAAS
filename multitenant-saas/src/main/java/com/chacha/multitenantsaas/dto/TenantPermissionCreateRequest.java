package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantPermissionCreateRequest(

        @NotBlank(
                message =
                        "Permission code is required"
        )
        @Size(
                max = 120,
                message =
                        "Permission code must not exceed "
                                + "120 characters"
        )
        String code,

        @NotBlank(
                message =
                        "Permission name is required"
        )
        @Size(
                max = 150,
                message =
                        "Permission name must not exceed "
                                + "150 characters"
        )
        String name,

        @Size(
                max = 500,
                message =
                        "Permission description must not "
                                + "exceed 500 characters"
        )
        String description,

        @NotBlank(
                message =
                        "Permission category is required"
        )
        @Size(
                max = 60,
                message =
                        "Permission category must not exceed "
                                + "60 characters"
        )
        String category
) {
}