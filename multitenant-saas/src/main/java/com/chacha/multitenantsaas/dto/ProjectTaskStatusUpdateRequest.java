package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectTaskStatusUpdateRequest(

        @NotNull(message = "Task status is required")
        ProjectTaskStatus status

) {
}