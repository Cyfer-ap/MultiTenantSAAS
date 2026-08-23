package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record TenantApiContextResponse(
        UUID tenantId, UUID apiKeyId, String apiKeyName, String keyPrefix) {}
