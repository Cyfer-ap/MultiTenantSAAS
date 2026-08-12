package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record TenantSubscriptionEntitlementResponse(
        UUID tenantId,
        UUID subscriptionId,
        UUID planId,
        String planCode,
        String planName,
        TenantSubscriptionStatus subscriptionStatus,
        SubscriptionAccessLevel accessLevel,
        SubscriptionAccessReason accessReason,
        boolean serviceAvailable,
        boolean mutationsAllowed,
        boolean cancelAtPeriodEnd,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        Instant evaluatedAt,
        ResourceEntitlement users,
        ResourceEntitlement projects) {

    public record ResourceEntitlement(
            long used,
            Long limit,
            Long remaining,
            boolean unlimited,
            boolean limitReached,
            boolean overLimit,
            boolean creationAllowed) {}
}
