package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record WorkspaceLoginOptionResponse(
        UUID tenantId,
        String name,
        String slug,
        WorkspaceAuthenticationMode authenticationMode,
        String identityProviderDisplayName) {

    public WorkspaceLoginOptionResponse(UUID tenantId, String name, String slug) {
        this(tenantId, name, slug, WorkspaceAuthenticationMode.PASSWORD_ONLY, null);
    }
}
