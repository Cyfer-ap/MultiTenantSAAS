package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record OutboundWebhookEndpointCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 2048) String url,
        Boolean enabled,
        @NotEmpty Set<OutboundWebhookEventType> events) {}
