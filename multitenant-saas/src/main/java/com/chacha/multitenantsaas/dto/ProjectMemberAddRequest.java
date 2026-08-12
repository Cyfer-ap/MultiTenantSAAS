package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProjectMemberAddRequest(
        @NotNull(message = "User ID is required") UUID userId,
        @NotNull(message = "Project member role is required") ProjectMemberRole role) {}
