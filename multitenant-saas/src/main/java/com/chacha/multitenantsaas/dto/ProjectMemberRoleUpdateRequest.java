package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRoleUpdateRequest(

        @NotNull(message = "Project member role is required")
        ProjectMemberRole role

) {
}