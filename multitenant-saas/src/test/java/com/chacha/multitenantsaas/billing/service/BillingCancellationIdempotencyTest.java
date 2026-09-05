package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingCancellationIdempotencyTest {

    @Test
    void treatsAlreadyCanceledProviderSubscriptionAsSuccessfulAndRepairsLocalState() {
        UUID tenantId = UUID.randomUUID();
        String subscriptionId = "sub_already_cancelled";
        Instant periodStart = Instant.parse("2026-09-05T13:00:00Z");
        Instant periodEnd = Instant.parse("2026-10-05T13:00:00Z");

        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription = mock(TenantSubscription.class);
        when(subscription.getStatus()).thenReturn(TenantSubscriptionStatus.ACTIVE);
        when(subscription.getBillingProvider()).thenReturn(BillingProviderType.STRIPE);
        when(subscription.getProviderSubscriptionId()).thenReturn(subscriptionId);
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));

        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        doThrow(new BillingProviderException("Stripe subscription cancellation failed", null))
                .when(stripe)
                .cancelSubscription(subscriptionId);
        when(stripe.fetchSubscription(subscriptionId))
                .thenReturn(
                        new BillingProviderSubscriptionSnapshot(
                                BillingProviderType.STRIPE,
                                subscriptionId,
                                "ENTERPRISE",
                                TenantSubscriptionStatus.CANCELLED,
                                periodStart,
                                periodEnd,
                                false));

        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(stripe)),
                        repository,
                        mock(BillingSubscriptionHistoryResolver.class));

        BillingCancellationResult result = service.requestCancellation(tenantId);

        assertThat(result.provider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(result.providerSubscriptionId()).isEqualTo(subscriptionId);
        verify(subscription).setStatus(TenantSubscriptionStatus.CANCELLED);
        verify(subscription).setCurrentPeriodStart(periodStart);
        verify(subscription).setCurrentPeriodEnd(periodEnd);
        verify(subscription).setCancelAtPeriodEnd(false);
        verify(subscription).setBillingProvider(BillingProviderType.STRIPE);
        verify(subscription).setProviderSubscriptionId(subscriptionId);
        verify(repository).save(subscription);
    }
}
