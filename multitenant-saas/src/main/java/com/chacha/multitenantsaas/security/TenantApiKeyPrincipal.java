package com.chacha.multitenantsaas.security;

import java.util.UUID;

public record TenantApiKeyPrincipal(
        UUID tenantId, UUID apiKeyId, String apiKeyName, String keyPrefix) {}
