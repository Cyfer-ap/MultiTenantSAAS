package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SubscriptionAccessLevel;
import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse.ResourceEntitlement;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SubscriptionEntitlementService {

    private final TenantLookupService tenantLookupService;
    private final TenantSubscriptionRepository
            tenantSubscriptionRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;

    public SubscriptionEntitlementService(
            TenantLookupService tenantLookupService,
            TenantSubscriptionRepository
                    tenantSubscriptionRepository,
            AppUserRepository appUserRepository,
            ProjectRepository projectRepository
    ) {
        this.tenantLookupService = tenantLookupService;
        this.tenantSubscriptionRepository =
                tenantSubscriptionRepository;
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public TenantSubscriptionEntitlementResponse evaluate(
            UUID tenantId
    ) {
        if (tenantId == null) {
            throw new IllegalArgumentException(
                    "Tenant id is required."
            );
        }

        tenantLookupService.ensureExists(tenantId);

        Instant evaluatedAt = Instant.now();
        long activeUsers = appUserRepository
                .countByTenantIdAndStatus(
                        tenantId,
                        UserStatus.ACTIVE
                );
        long nonArchivedProjects = Math.max(
                0L,
                projectRepository.countByTenant_Id(tenantId)
                        - projectRepository
                        .countByTenant_IdAndStatus(
                                tenantId,
                                ProjectStatus.ARCHIVED
                        )
        );

        return tenantSubscriptionRepository
                .findByTenantIdWithPlan(tenantId)
                .map(
                        subscription -> evaluateSubscription(
                                tenantId,
                                subscription,
                                activeUsers,
                                nonArchivedProjects,
                                evaluatedAt
                        )
                )
                .orElseGet(
                        () -> noSubscriptionResponse(
                                tenantId,
                                activeUsers,
                                nonArchivedProjects,
                                evaluatedAt
                        )
                );
    }

    private TenantSubscriptionEntitlementResponse
    evaluateSubscription(
            UUID tenantId,
            TenantSubscription subscription,
            long activeUsers,
            long nonArchivedProjects,
            Instant evaluatedAt
    ) {
        SubscriptionPlan plan = subscription.getPlan();
        AccessDecision accessDecision = evaluateAccess(
                subscription,
                plan,
                evaluatedAt
        );

        return new TenantSubscriptionEntitlementResponse(
                tenantId,
                subscription.getId(),
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                subscription.getStatus(),
                accessDecision.accessLevel(),
                accessDecision.accessReason(),
                accessDecision.serviceAvailable(),
                accessDecision.mutationsAllowed(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCurrentPeriodEnd(),
                subscription.getTrialEndsAt(),
                evaluatedAt,
                resourceEntitlement(
                        activeUsers,
                        toLong(plan.getMaxUsers()),
                        accessDecision.mutationsAllowed()
                ),
                resourceEntitlement(
                        nonArchivedProjects,
                        toLong(plan.getMaxProjects()),
                        accessDecision.mutationsAllowed()
                )
        );
    }

    private TenantSubscriptionEntitlementResponse
    noSubscriptionResponse(
            UUID tenantId,
            long activeUsers,
            long nonArchivedProjects,
            Instant evaluatedAt
    ) {
        return new TenantSubscriptionEntitlementResponse(
                tenantId,
                null,
                null,
                null,
                null,
                null,
                SubscriptionAccessLevel.BLOCKED,
                SubscriptionAccessReason.NO_SUBSCRIPTION,
                false,
                false,
                false,
                null,
                null,
                evaluatedAt,
                unavailableResourceEntitlement(activeUsers),
                unavailableResourceEntitlement(
                        nonArchivedProjects
                )
        );
    }

    private AccessDecision evaluateAccess(
            TenantSubscription subscription,
            SubscriptionPlan plan,
            Instant evaluatedAt
    ) {
        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            return AccessDecision.blocked(
                    SubscriptionAccessReason.PLAN_INACTIVE
            );
        }

        TenantSubscriptionStatus status =
                subscription.getStatus();

        if (status == TenantSubscriptionStatus.CANCELLED) {
            return AccessDecision.blocked(
                    SubscriptionAccessReason.CANCELLED
            );
        }

        if (status == TenantSubscriptionStatus.EXPIRED) {
            return AccessDecision.blocked(
                    SubscriptionAccessReason.EXPIRED
            );
        }

        Instant currentPeriodEnd =
                subscription.getCurrentPeriodEnd();

        if (
                currentPeriodEnd == null
                        || !currentPeriodEnd.isAfter(evaluatedAt)
        ) {
            return AccessDecision.blocked(
                    SubscriptionAccessReason.PERIOD_EXPIRED
            );
        }

        if (status == TenantSubscriptionStatus.TRIALING) {
            Instant trialEndsAt = subscription.getTrialEndsAt();

            if (
                    trialEndsAt == null
                            || !trialEndsAt.isAfter(evaluatedAt)
            ) {
                return AccessDecision.blocked(
                        SubscriptionAccessReason.TRIAL_EXPIRED
                );
            }

            return AccessDecision.fullAccess(
                    SubscriptionAccessReason.TRIAL_ACTIVE
            );
        }

        if (status == TenantSubscriptionStatus.PAST_DUE) {
            return new AccessDecision(
                    SubscriptionAccessLevel.GRACE_ACCESS,
                    SubscriptionAccessReason.PAST_DUE_GRACE,
                    true,
                    true
            );
        }

        return AccessDecision.fullAccess(
                SubscriptionAccessReason.ACTIVE
        );
    }

    private ResourceEntitlement unavailableResourceEntitlement(
            long used
    ) {
        return new ResourceEntitlement(
                used,
                null,
                null,
                false,
                false,
                false,
                false
        );
    }

    private ResourceEntitlement resourceEntitlement(
            long used,
            Long limit,
            boolean mutationsAllowed
    ) {
        if (limit == null) {
            return new ResourceEntitlement(
                    used,
                    null,
                    null,
                    true,
                    false,
                    false,
                    mutationsAllowed
            );
        }

        boolean limitReached = used >= limit;
        boolean overLimit = used > limit;
        long remaining = Math.max(0L, limit - used);

        return new ResourceEntitlement(
                used,
                limit,
                remaining,
                false,
                limitReached,
                overLimit,
                mutationsAllowed && !limitReached
        );
    }

    private Long toLong(Integer value) {
        return value == null
                ? null
                : value.longValue();
    }

    private record AccessDecision(
            SubscriptionAccessLevel accessLevel,
            SubscriptionAccessReason accessReason,
            boolean serviceAvailable,
            boolean mutationsAllowed
    ) {

        private static AccessDecision fullAccess(
                SubscriptionAccessReason accessReason
        ) {
            return new AccessDecision(
                    SubscriptionAccessLevel.FULL_ACCESS,
                    accessReason,
                    true,
                    true
            );
        }

        private static AccessDecision blocked(
                SubscriptionAccessReason accessReason
        ) {
            return new AccessDecision(
                    SubscriptionAccessLevel.BLOCKED,
                    accessReason,
                    false,
                    false
            );
        }
    }
}
