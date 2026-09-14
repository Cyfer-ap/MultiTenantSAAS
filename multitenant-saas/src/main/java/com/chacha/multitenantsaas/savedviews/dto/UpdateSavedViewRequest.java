package com.chacha.multitenantsaas.savedviews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateSavedViewRequest(
        @NotBlank @Size(max = 80) String name, @NotNull Map<String, String> definition) {}
