package com.chacha.multitenantsaas.service;

import java.util.UUID;

public record TenantIdentityProviderVerificationSnapshot(
        UUID identityProviderId, long version, OidcProviderVerificationInput input) {}
