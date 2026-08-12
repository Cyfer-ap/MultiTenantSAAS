package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionLifecycleGuardService {

    private final SubscriptionEntitlementService subscriptionEntitlementService;
    private final boolean enforcementEnabled;

    @Autowired
    public SubscriptionLifecycleGuardService(
            SubscriptionEntitlementService subscriptionEntitlementService,
            Environment environment) {
        this(subscriptionEntitlementService, resolveEnforcementEnabled(environment));
    }

    SubscriptionLifecycleGuardService(
            SubscriptionEntitlementService subscriptionEntitlementService,
            boolean enforcementEnabled) {
        this.subscriptionEntitlementService = subscriptionEntitlementService;
        this.enforcementEnabled = enforcementEnabled;
    }

    public void requireBusinessMutationAllowed(UUID tenantId) {
        if (!enforcementEnabled) {
            return;
        }

        TenantSubscriptionEntitlementResponse entitlements =
                subscriptionEntitlementService.evaluate(tenantId);

        if (entitlements.mutationsAllowed()) {
            return;
        }

        throw new SubscriptionRestrictionException(
                RestrictionType.WORKSPACE_READ_ONLY,
                entitlements.accessReason(),
                "workspace",
                null,
                null,
                "This workspace is read-only because "
                        + describeAccessReason(entitlements.accessReason())
                        + ". Read operations and permitted "
                        + "recovery actions remain available.");
    }

    private static boolean resolveEnforcementEnabled(Environment environment) {
        String configured = environment.getProperty("app.subscription.enforcement.enabled");

        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }

        for (String profile : environment.getActiveProfiles()) {
            if ("test".equals(profile)) {
                return false;
            }
        }

        return true;
    }

    private String describeAccessReason(SubscriptionAccessReason accessReason) {
        if (accessReason == null) {
            return "its subscription does not allow mutations";
        }

        return switch (accessReason) {
            case NO_SUBSCRIPTION -> "it does not have a subscription";
            case PLAN_INACTIVE -> "its subscription plan is inactive";
            case CANCELLED -> "its subscription has been cancelled";
            case EXPIRED -> "its subscription has expired";
            case PERIOD_EXPIRED -> "its current billing period has ended";
            case TRIAL_EXPIRED -> "its trial has ended";
            case ACTIVE, TRIAL_ACTIVE, PAST_DUE_GRACE ->
                    "its subscription does not allow mutations";
        };
    }
}
