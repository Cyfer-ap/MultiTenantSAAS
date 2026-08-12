package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.SubscriptionAccessLevel;
import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionEntitlementServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    @Mock private TenantLookupService tenantLookupService;

    @Mock private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Mock private AppUserRepository appUserRepository;

    @Mock private ProjectRepository projectRepository;

    @Mock private TenantSubscription subscription;

    @Mock private SubscriptionPlan plan;

    private SubscriptionEntitlementService service;

    @BeforeEach
    void setUp() {
        service =
                new SubscriptionEntitlementService(
                        tenantLookupService,
                        tenantSubscriptionRepository,
                        appUserRepository,
                        projectRepository);
    }

    @Test
    void activeSubscriptionEvaluatesUsageAndQuotaAvailability() {
        stubUsage(2L, 2L, 0L);
        stubSubscription(
                TenantSubscriptionStatus.ACTIVE,
                SubscriptionPlanStatus.ACTIVE,
                Instant.now().plus(30, ChronoUnit.DAYS),
                null,
                3,
                2);

        TenantSubscriptionEntitlementResponse result = service.evaluate(TENANT_ID);

        assertThat(result.accessLevel()).isEqualTo(SubscriptionAccessLevel.FULL_ACCESS);
        assertThat(result.accessReason()).isEqualTo(SubscriptionAccessReason.ACTIVE);
        assertThat(result.serviceAvailable()).isTrue();
        assertThat(result.mutationsAllowed()).isTrue();
        assertThat(result.users().used()).isEqualTo(2L);
        assertThat(result.users().remaining()).isEqualTo(1L);
        assertThat(result.users().creationAllowed()).isTrue();
        assertThat(result.projects().limitReached()).isTrue();
        assertThat(result.projects().creationAllowed()).isFalse();
    }

    @Test
    void activeTrialAllowsMutationsBeforeTrialEnd() {
        stubUsage(1L, 0L, 0L);
        stubSubscription(
                TenantSubscriptionStatus.TRIALING,
                SubscriptionPlanStatus.ACTIVE,
                Instant.now().plus(30, ChronoUnit.DAYS),
                Instant.now().plus(7, ChronoUnit.DAYS),
                null,
                null);

        TenantSubscriptionEntitlementResponse result = service.evaluate(TENANT_ID);

        assertThat(result.accessLevel()).isEqualTo(SubscriptionAccessLevel.FULL_ACCESS);
        assertThat(result.accessReason()).isEqualTo(SubscriptionAccessReason.TRIAL_ACTIVE);
        assertThat(result.users().unlimited()).isTrue();
        assertThat(result.users().creationAllowed()).isTrue();
    }

    @Test
    void expiredTrialBlocksServiceAndCreation() {
        stubUsage(1L, 1L, 0L);
        stubSubscription(
                TenantSubscriptionStatus.TRIALING,
                SubscriptionPlanStatus.ACTIVE,
                Instant.now().plus(30, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.HOURS),
                10,
                10);

        TenantSubscriptionEntitlementResponse result = service.evaluate(TENANT_ID);

        assertThat(result.accessLevel()).isEqualTo(SubscriptionAccessLevel.BLOCKED);
        assertThat(result.accessReason()).isEqualTo(SubscriptionAccessReason.TRIAL_EXPIRED);
        assertThat(result.serviceAvailable()).isFalse();
        assertThat(result.mutationsAllowed()).isFalse();
        assertThat(result.users().creationAllowed()).isFalse();
        assertThat(result.projects().creationAllowed()).isFalse();
    }

    @Test
    void pastDueSubscriptionUsesGraceAccessUntilPeriodEnd() {
        stubUsage(4L, 3L, 1L);
        stubSubscription(
                TenantSubscriptionStatus.PAST_DUE,
                SubscriptionPlanStatus.ACTIVE,
                Instant.now().plus(3, ChronoUnit.DAYS),
                null,
                10,
                10);

        TenantSubscriptionEntitlementResponse result = service.evaluate(TENANT_ID);

        assertThat(result.accessLevel()).isEqualTo(SubscriptionAccessLevel.GRACE_ACCESS);
        assertThat(result.accessReason()).isEqualTo(SubscriptionAccessReason.PAST_DUE_GRACE);
        assertThat(result.serviceAvailable()).isTrue();
        assertThat(result.mutationsAllowed()).isTrue();
    }

    @Test
    void missingSubscriptionReturnsBlockedSnapshotWithCurrentUsage() {
        stubUsage(5L, 4L, 1L);
        when(tenantSubscriptionRepository.findByTenantIdWithPlan(TENANT_ID))
                .thenReturn(Optional.empty());

        TenantSubscriptionEntitlementResponse result = service.evaluate(TENANT_ID);

        assertThat(result.subscriptionId()).isNull();
        assertThat(result.planId()).isNull();
        assertThat(result.accessLevel()).isEqualTo(SubscriptionAccessLevel.BLOCKED);
        assertThat(result.accessReason()).isEqualTo(SubscriptionAccessReason.NO_SUBSCRIPTION);
        assertThat(result.users().used()).isEqualTo(5L);
        assertThat(result.users().unlimited()).isFalse();
        assertThat(result.users().creationAllowed()).isFalse();
        assertThat(result.projects().used()).isEqualTo(3L);

        verify(tenantLookupService).ensureExists(TENANT_ID);
    }

    @Test
    void inactivePlanBlocksOtherwiseActiveSubscription() {
        stubUsage(1L, 1L, 0L);
        stubSubscription(
                TenantSubscriptionStatus.ACTIVE,
                SubscriptionPlanStatus.INACTIVE,
                Instant.now().plus(30, ChronoUnit.DAYS),
                null,
                10,
                10);

        TenantSubscriptionEntitlementResponse result = service.evaluate(TENANT_ID);

        assertThat(result.accessLevel()).isEqualTo(SubscriptionAccessLevel.BLOCKED);
        assertThat(result.accessReason()).isEqualTo(SubscriptionAccessReason.PLAN_INACTIVE);
        assertThat(result.mutationsAllowed()).isFalse();
    }

    private void stubUsage(long activeUsers, long totalProjects, long archivedProjects) {
        when(appUserRepository.countByTenantIdAndStatus(TENANT_ID, UserStatus.ACTIVE))
                .thenReturn(activeUsers);
        when(projectRepository.countByTenant_Id(TENANT_ID)).thenReturn(totalProjects);
        when(projectRepository.countByTenant_IdAndStatus(TENANT_ID, ProjectStatus.ARCHIVED))
                .thenReturn(archivedProjects);
    }

    private void stubSubscription(
            TenantSubscriptionStatus subscriptionStatus,
            SubscriptionPlanStatus planStatus,
            Instant currentPeriodEnd,
            Instant trialEndsAt,
            Integer maxUsers,
            Integer maxProjects) {
        when(tenantSubscriptionRepository.findByTenantIdWithPlan(TENANT_ID))
                .thenReturn(Optional.of(subscription));
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getPlan()).thenReturn(plan);
        when(subscription.getStatus()).thenReturn(subscriptionStatus);
        when(subscription.getCurrentPeriodEnd()).thenReturn(currentPeriodEnd);
        when(subscription.getTrialEndsAt()).thenReturn(trialEndsAt);
        when(subscription.isCancelAtPeriodEnd()).thenReturn(false);
        when(plan.getId()).thenReturn(PLAN_ID);
        when(plan.getCode()).thenReturn("GROWTH");
        when(plan.getName()).thenReturn("Growth");
        when(plan.getStatus()).thenReturn(planStatus);
        when(plan.getMaxUsers()).thenReturn(maxUsers);
        when(plan.getMaxProjects()).thenReturn(maxProjects);
    }
}
