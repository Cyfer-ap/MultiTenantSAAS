package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionEventMapper;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import com.chacha.multitenantsaas.service.TenantLookupService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingSubscriptionSynchronizerCrossProviderTest {

    @Test
    void doesNotLetFailedStripeSubscriptionReplaceActiveRazorpaySubscription() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);
        TenantSubscription subscription =
                new TenantSubscription(
                        tenant,
                        mock(SubscriptionPlan.class),
                        TenantSubscriptionStatus.ACTIVE,
                        Instant.parse("2026-08-27T12:33:00Z"),
                        Instant.parse("2026-08-27T12:33:00Z"),
                        Instant.parse("2026-09-27T12:33:00Z"),
                        null,
                        false);
        subscription.setBillingProvider(BillingProviderType.RAZORPAY);
        subscription.setProviderSubscriptionId("sub_razorpay_active");
        subscription.setProviderEventCreatedAt(Instant.parse("2026-08-27T12:34:00Z"));

        BillingSubscriptionUpdate failedStripeUpdate =
                new BillingSubscriptionUpdate(
                        BillingProviderType.STRIPE,
                        "sub_stripe_incomplete",
                        tenantId,
                        "PRO",
                        TenantSubscriptionStatus.PAST_DUE,
                        Instant.parse("2026-08-27T12:35:00Z"),
                        Instant.parse("2026-08-27T12:35:00Z"),
                        Instant.parse("2026-09-27T12:35:00Z"),
                        null,
                        false,
                        Instant.parse("2026-08-27T12:35:00Z"));
        VerifiedBillingEvent event =
                new VerifiedBillingEvent(
                        BillingProviderType.STRIPE,
                        "evt_stripe_incomplete",
                        "customer.subscription.created",
                        "{}");
        BillingSubscriptionEventMapper mapper = mock(BillingSubscriptionEventMapper.class);
        when(mapper.providerType()).thenReturn(BillingProviderType.STRIPE);
        when(mapper.map(event)).thenReturn(Optional.of(failedStripeUpdate));

        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        when(repository.findByTenantIdWithPlanForUpdate(tenantId))
                .thenReturn(Optional.of(subscription));
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        BillingSubscriptionSynchronizer synchronizer =
                new BillingSubscriptionSynchronizer(
                        List.of(mapper), repository, mock(TenantLookupService.class), planService);

        synchronizer.synchronize(event);

        assertThat(subscription.getStatus()).isEqualTo(TenantSubscriptionStatus.ACTIVE);
        assertThat(subscription.getBillingProvider()).isEqualTo(BillingProviderType.RAZORPAY);
        assertThat(subscription.getProviderSubscriptionId()).isEqualTo("sub_razorpay_active");
        verify(planService, never()).getActivePlanEntityByCode("PRO");
        verify(repository, never()).save(subscription);
    }
}
