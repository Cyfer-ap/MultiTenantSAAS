package com.chacha.multitenantsaas.externalaccess;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

record ExternalGuestSessionContext(
        UUID grantId,
        UUID tenantId,
        UUID projectId,
        String guestName,
        String guestEmail,
        Set<ExternalAccessCapability> capabilities,
        Instant grantExpiresAt,
        Instant sessionExpiresAt) {

    void require(ExternalAccessCapability capability) {
        if (!capabilities.contains(capability)) {
            throw new IllegalArgumentException("Guest grant does not allow this operation");
        }
    }
}
