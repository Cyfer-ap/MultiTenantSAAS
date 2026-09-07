package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.catalog.SubscriptionPlanCatalogService;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionPlanRetirementCoordinatorTest {

    @Test
    void archivesCatalogAndSchedulesOnlySubscriptionsNotAlreadyEnding() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        SubscriptionPlanCatalogService catalogService = mock(SubscriptionPlanCatalogService.class);
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        SubscriptionPlanRetirementOperationService operationService =
                mock(SubscriptionPlanRetirementOperationService.class);
        SubscriptionPlanResponse plan = mock(SubscriptionPlanResponse.class);
        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingProviderRegistry registry = new BillingProviderRegistry(List.of(stripe));
        when(planService.getPlan(planId)).thenReturn(plan);

        TenantSubscription needsCancellation = subscription("sub_1");
        TenantSubscription alreadyEnding = subscription("sub_2");
        when(repository.findProviderLinkedSubscriptionsForPlan(
                        org.mockito.ArgumentMatchers.eq(planId),
                        org.mockito.ArgumentMatchers.<Collection<TenantSubscriptionStatus>>any()))
                .thenReturn(List.of(needsCancellation, alreadyEnding));
        when(stripe.fetchSubscription("sub_1"))
                .thenReturn(snapshot("sub_1", false, TenantSubscriptionStatus.ACTIVE));
        when(stripe.fetchSubscription("sub_2"))
                .thenReturn(snapshot("sub_2", true, TenantSubscriptionStatus.ACTIVE));

        SubscriptionPlanRetirementCoordinator coordinator =
                new SubscriptionPlanRetirementCoordinator(
                        planService, catalogService, registry, repository, operationService);

        coordinator.execute(planId);

        verify(operationService).markProcessing(planId);
        verify(catalogService).planRetired(plan);
        verify(stripe).scheduleCancellationAtPeriodEnd("sub_1");
        verify(stripe, never()).scheduleCancellationAtPeriodEnd("sub_2");
        verify(operationService).markCompleted(planId);
    }

    @Test
    void providerFailureIsPersistedAfterOtherSubscriptionsAreAttempted() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        SubscriptionPlanCatalogService catalogService = mock(SubscriptionPlanCatalogService.class);
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        SubscriptionPlanRetirementOperationService operationService =
                mock(SubscriptionPlanRetirementOperationService.class);
        SubscriptionPlanResponse plan = mock(SubscriptionPlanResponse.class);
        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingProviderRegistry registry = new BillingProviderRegistry(List.of(stripe));
        when(planService.getPlan(planId)).thenReturn(plan);

        TenantSubscription first = subscription("sub_fail");
        TenantSubscription second = subscription("sub_ok");
        when(repository.findProviderLinkedSubscriptionsForPlan(
                        org.mockito.ArgumentMatchers.eq(planId),
                        org.mockito.ArgumentMatchers.<Collection<TenantSubscriptionStatus>>any()))
                .thenReturn(List.of(first, second));
        when(stripe.fetchSubscription("sub_fail"))
                .thenReturn(snapshot("sub_fail", false, TenantSubscriptionStatus.ACTIVE));
        when(stripe.fetchSubscription("sub_ok"))
                .thenReturn(snapshot("sub_ok", false, TenantSubscriptionStatus.ACTIVE));
        doThrow(new BillingProviderException("provider unavailable", null))
                .when(stripe)
                .scheduleCancellationAtPeriodEnd("sub_fail");

        SubscriptionPlanRetirementCoordinator coordinator =
                new SubscriptionPlanRetirementCoordinator(
                        planService, catalogService, registry, repository, operationService);

        assertThatThrownBy(() -> coordinator.execute(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retired locally");

        verify(stripe).scheduleCancellationAtPeriodEnd("sub_ok");
        verify(operationService)
                .markFailed(
                        org.mockito.ArgumentMatchers.eq(planId),
                        org.mockito.ArgumentMatchers.any());
        verify(operationService, never()).markCompleted(planId);
    }

    private TenantSubscription subscription(String providerId) {
        TenantSubscription subscription = mock(TenantSubscription.class);
        when(subscription.getBillingProvider()).thenReturn(BillingProviderType.STRIPE);
        when(subscription.getProviderSubscriptionId()).thenReturn(providerId);
        return subscription;
    }

    private BillingProviderSubscriptionSnapshot snapshot(
            String id, boolean cancelAtPeriodEnd, TenantSubscriptionStatus status) {
        return new BillingProviderSubscriptionSnapshot(
                BillingProviderType.STRIPE,
                id,
                "PRO",
                status,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-10-01T00:00:00Z"),
                cancelAtPeriodEnd);
    }
}
