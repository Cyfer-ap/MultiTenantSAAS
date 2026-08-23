package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingReconciliationMismatch;
import com.chacha.multitenantsaas.billing.dto.BillingReconciliationResponse;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingReconciliationServiceTest {

    @Test
    void reportsConsistentProviderStateWithoutMutatingSubscription() {
        UUID tenantId = UUID.randomUUID();
        Instant periodStart = Instant.parse("2026-08-23T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-09-23T00:00:00Z");
        TenantSubscription subscription =
                subscription(
                        "PRO",
                        TenantSubscriptionStatus.ACTIVE,
                        periodStart,
                        periodEnd,
                        false);

        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));
        BillingProvider provider = mock(BillingProvider.class);
        when(provider.providerType()).thenReturn(BillingProviderType.STRIPE);
        when(provider.fetchSubscription("sub_123"))
                .thenReturn(
                        new BillingProviderSubscriptionSnapshot(
                                BillingProviderType.STRIPE,
                                "sub_123",
                                "PRO",
                                TenantSubscriptionStatus.ACTIVE,
                                periodStart,
                                periodEnd,
                                false));
        BillingReconciliationService service =
                new BillingReconciliationService(
                        new BillingProviderRegistry(List.of(provider)), repository);

        BillingReconciliationResponse response = service.reconcile(tenantId);

        assertThat(response.consistent()).isTrue();
        assertThat(response.mismatches()).isEmpty();
        assertThat(response.checkedAt()).isNotNull();
        verify(provider).fetchSubscription("sub_123");
    }

    @Test
    void reportsEveryMaterialProviderMismatch() {
        UUID tenantId = UUID.randomUUID();
        Instant periodStart = Instant.parse("2026-08-23T00:00:00Z");
        Instant localPeriodEnd = Instant.parse("2026-09-23T00:00:00Z");
        Instant providerPeriodEnd = Instant.parse("2026-10-23T00:00:00Z");
        TenantSubscription subscription =
                subscription(
                        "PRO",
                        TenantSubscriptionStatus.ACTIVE,
                        periodStart,
                        localPeriodEnd,
                        false);

        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));
        BillingProvider provider = mock(BillingProvider.class);
        when(provider.providerType()).thenReturn(BillingProviderType.STRIPE);
        when(provider.fetchSubscription("sub_123"))
                .thenReturn(
                        new BillingProviderSubscriptionSnapshot(
                                BillingProviderType.STRIPE,
                                "sub_123",
                                "ENTERPRISE",
                                TenantSubscriptionStatus.PAST_DUE,
                                periodStart,
                                providerPeriodEnd,
                                true));
        BillingReconciliationService service =
                new BillingReconciliationService(
                        new BillingProviderRegistry(List.of(provider)), repository);

        BillingReconciliationResponse response = service.reconcile(tenantId);

        assertThat(response.consistent()).isFalse();
        assertThat(response.mismatches())
                .containsExactly(
                        BillingReconciliationMismatch.PLAN_CODE,
                        BillingReconciliationMismatch.STATUS,
                        BillingReconciliationMismatch.CURRENT_PERIOD_END,
                        BillingReconciliationMismatch.CANCEL_AT_PERIOD_END);
        assertThat(response.localStatus()).isEqualTo(TenantSubscriptionStatus.ACTIVE);
        assertThat(response.providerStatus()).isEqualTo(TenantSubscriptionStatus.PAST_DUE);
    }

    private TenantSubscription subscription(
            String planCode,
            TenantSubscriptionStatus status,
            Instant periodStart,
            Instant periodEnd,
            boolean cancelAtPeriodEnd) {
        SubscriptionPlan plan = mock(SubscriptionPlan.class);
        when(plan.getCode()).thenReturn(planCode);

        TenantSubscription subscription = mock(TenantSubscription.class);
        when(subscription.getPlan()).thenReturn(plan);
        when(subscription.getStatus()).thenReturn(status);
        when(subscription.getBillingProvider()).thenReturn(BillingProviderType.STRIPE);
        when(subscription.getProviderSubscriptionId()).thenReturn("sub_123");
        when(subscription.getCurrentPeriodStart()).thenReturn(periodStart);
        when(subscription.getCurrentPeriodEnd()).thenReturn(periodEnd);
        when(subscription.isCancelAtPeriodEnd()).thenReturn(cancelAtPeriodEnd);
        return subscription;
    }
}
