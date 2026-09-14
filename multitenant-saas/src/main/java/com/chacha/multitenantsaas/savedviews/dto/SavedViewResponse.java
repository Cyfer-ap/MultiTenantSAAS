package com.chacha.multitenantsaas.savedviews.dto;

import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SavedViewResponse(
        UUID id,
        String name,
        SavedViewTarget target,
        UUID contextId,
        Map<String, String> definition,
        Instant createdAt,
        Instant updatedAt) {}
