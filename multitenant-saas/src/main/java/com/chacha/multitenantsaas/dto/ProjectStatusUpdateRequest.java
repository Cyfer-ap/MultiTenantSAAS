package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectStatusUpdateRequest(

        @NotNull(message = "Project status is required")
        ProjectStatus status

) {
}