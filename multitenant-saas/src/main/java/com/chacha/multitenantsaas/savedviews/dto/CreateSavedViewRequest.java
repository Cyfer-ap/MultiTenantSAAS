package com.chacha.multitenantsaas.savedviews.dto;

import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record CreateSavedViewRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull SavedViewTarget target,
        UUID contextId,
        @NotNull Map<String, String> definition) {}
