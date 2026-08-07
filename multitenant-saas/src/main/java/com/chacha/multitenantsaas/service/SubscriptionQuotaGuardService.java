package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse.ResourceEntitlement;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubscriptionQuotaGuardService {

    private final SubscriptionEntitlementService
            subscriptionEntitlementService;
    private final TenantSubscriptionRepository
            tenantSubscriptionRepository;
    private final boolean enforcementEnabled;

    @Autowired
    public SubscriptionQuotaGuardService(
            SubscriptionEntitlementService
                    subscriptionEntitlementService,
            TenantSubscriptionRepository
                    tenantSubscriptionRepository,
            Environment environment
    ) {
        this(
                subscriptionEntitlementService,
                tenantSubscriptionRepository,
                resolveEnforcementEnabled(environment)
        );
    }

    SubscriptionQuotaGuardService(
            SubscriptionEntitlementService
                    subscriptionEntitlementService,
            TenantSubscriptionRepository
                    tenantSubscriptionRepository,
            boolean enforcementEnabled
    ) {
        this.subscriptionEntitlementService =
                subscriptionEntitlementService;
        this.tenantSubscriptionRepository =
                tenantSubscriptionRepository;
        this.enforcementEnabled = enforcementEnabled;
    }

    private static boolean resolveEnforcementEnabled(
            Environment environment
    ) {
        String configured = environment.getProperty(
                "app.subscription.enforcement.enabled"
        );

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

    @Transactional
    public void requireUserSlot(UUID tenantId) {
        requireCapacity(
                tenantId,
                ResourceType.USERS
        );
    }

    @Transactional
    public void requireProjectSlot(UUID tenantId) {
        requireCapacity(
                tenantId,
                ResourceType.PROJECTS
        );
    }

    private void requireCapacity(
            UUID tenantId,
            ResourceType resourceType
    ) {
        if (!enforcementEnabled) {
            return;
        }

        /*
         * Serialize quota-increasing operations for one tenant.
         * The lock is held by the surrounding transaction until
         * the user/project mutation commits or rolls back.
         */
        tenantSubscriptionRepository
                .findByTenantIdWithPlanForUpdate(tenantId);

        TenantSubscriptionEntitlementResponse entitlements =
                subscriptionEntitlementService.evaluate(tenantId);

        if (!entitlements.mutationsAllowed()) {
            throw serviceUnavailable(
                    entitlements.accessReason(),
                    resourceType
            );
        }

        ResourceEntitlement resourceEntitlement =
                resourceType == ResourceType.USERS
                        ? entitlements.users()
                        : entitlements.projects();

        if (resourceEntitlement.creationAllowed()) {
            return;
        }

        throw quotaReached(
                entitlements.accessReason(),
                resourceType,
                resourceEntitlement
        );
    }

    private SubscriptionRestrictionException
    serviceUnavailable(
            SubscriptionAccessReason accessReason,
            ResourceType resourceType
    ) {
        String action = resourceType == ResourceType.USERS
                ? "create or reactivate users"
                : "create projects";

        return new SubscriptionRestrictionException(
                RestrictionType.SERVICE_UNAVAILABLE,
                accessReason,
                resourceType.apiName,
                null,
                null,
                "This workspace cannot "
                        + action
                        + " because "
                        + describeAccessReason(accessReason)
                        + "."
        );
    }

    private SubscriptionRestrictionException quotaReached(
            SubscriptionAccessReason accessReason,
            ResourceType resourceType,
            ResourceEntitlement entitlement
    ) {
        Long used = entitlement.used();
        Long limit = entitlement.limit();

        String message;

        if (resourceType == ResourceType.USERS) {
            message = limit == null
                    ? "The current subscription does not allow another active user."
                    : "User limit reached for the current subscription plan: "
                    + used
                    + " of "
                    + limit
                    + " active users are in use.";
        } else {
            message = limit == null
                    ? "The current subscription does not allow another project."
                    : "Project limit reached for the current subscription plan: "
                    + used
                    + " of "
                    + limit
                    + " non-archived projects are in use.";
        }

        return new SubscriptionRestrictionException(
                resourceType.restrictionType,
                accessReason,
                resourceType.apiName,
                used,
                limit,
                message
        );
    }

    private String describeAccessReason(
            SubscriptionAccessReason accessReason
    ) {
        if (accessReason == null) {
            return "its subscription does not currently allow mutations";
        }

        return switch (accessReason) {
            case NO_SUBSCRIPTION ->
                    "it does not have a subscription";
            case PLAN_INACTIVE ->
                    "its subscription plan is inactive";
            case CANCELLED ->
                    "its subscription has been cancelled";
            case EXPIRED ->
                    "its subscription has expired";
            case PERIOD_EXPIRED ->
                    "its current billing period has ended";
            case TRIAL_EXPIRED ->
                    "its trial has ended";
            case ACTIVE, TRIAL_ACTIVE, PAST_DUE_GRACE ->
                    "its subscription does not currently allow mutations";
        };
    }

    private enum ResourceType {
        USERS(
                "users",
                RestrictionType.USER_LIMIT_REACHED
        ),
        PROJECTS(
                "projects",
                RestrictionType.PROJECT_LIMIT_REACHED
        );

        private final String apiName;
        private final RestrictionType restrictionType;

        ResourceType(
                String apiName,
                RestrictionType restrictionType
        ) {
            this.apiName = apiName;
            this.restrictionType = restrictionType;
        }
    }
}
