package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record OutboundWebhookEndpointUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 2048) String url,
        @NotNull Boolean enabled,
        @NotEmpty Set<OutboundWebhookEventType> events) {}
