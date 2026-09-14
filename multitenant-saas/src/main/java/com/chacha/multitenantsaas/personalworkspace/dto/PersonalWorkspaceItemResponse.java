package com.chacha.multitenantsaas.personalworkspace.dto;

import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
import java.time.Instant;
import java.util.UUID;

public record PersonalWorkspaceItemResponse(
        PersonalResourceType type,
        UUID resourceId,
        UUID parentId,
        String title,
        String subtitle,
        Instant favoriteAt,
        Instant lastViewedAt) {}
