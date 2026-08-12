package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record TenantSubscriptionAccessResponse(
        UUID tenantId,
        TenantSubscriptionStatus subscriptionStatus,
        SubscriptionAccessLevel accessLevel,
        SubscriptionAccessReason accessReason,
        boolean serviceAvailable,
        boolean mutationsAllowed,
        boolean userCreationAllowed,
        boolean projectCreationAllowed,
        boolean userLimitReached,
        boolean projectLimitReached,
        boolean cancelAtPeriodEnd,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        Instant evaluatedAt) {

    public static TenantSubscriptionAccessResponse from(
            TenantSubscriptionEntitlementResponse entitlements) {
        if (entitlements == null) {
            throw new IllegalArgumentException("Subscription entitlements are required.");
        }

        return new TenantSubscriptionAccessResponse(
                entitlements.tenantId(),
                entitlements.subscriptionStatus(),
                entitlements.accessLevel(),
                entitlements.accessReason(),
                entitlements.serviceAvailable(),
                entitlements.mutationsAllowed(),
                entitlements.users().creationAllowed(),
                entitlements.projects().creationAllowed(),
                entitlements.users().limitReached(),
                entitlements.projects().limitReached(),
                entitlements.cancelAtPeriodEnd(),
                entitlements.currentPeriodEnd(),
                entitlements.trialEndsAt(),
                entitlements.evaluatedAt());
    }
}
