package com.chacha.multitenantsaas.search.spi;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import java.util.List;
import java.util.UUID;

public record GlobalSearchContext(UUID tenantId, UUID userId, List<Grant> grants) {
    public record Grant(
            AuthorizationScopeType scopeType, UUID scopeTargetId, List<String> permissionCodes) {}
}
