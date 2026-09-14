package com.chacha.multitenantsaas.calendar.spi;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import java.util.List;
import java.util.UUID;

public record CalendarDeadlineContext(UUID tenantId, UUID userId, List<Grant> grants) {

    public record Grant(
            AuthorizationScopeType scopeType, UUID scopeTargetId, List<String> permissionCodes) {}
}
