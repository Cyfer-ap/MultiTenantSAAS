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

class BillingSubscriptionSynchronizerTest {

    @Test
    void appliesNewerProviderLifecycleUpdate() {
        UUID tenantId = UUID.randomUUID();
        BillingSubscriptionUpdate update = update(tenantId, Instant.parse("2026-08-22T12:00:00Z"));
        BillingSubscriptionEventMapper mapper = mapper(update);
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription = subscription(tenantId);
        when(repository.findByTenantIdWithPlanForUpdate(tenantId))
                .thenReturn(Optional.of(subscription));
        SubscriptionPlan targetPlan = mock(SubscriptionPlan.class);
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        when(planService.getActivePlanEntityByCode("PRO")).thenReturn(targetPlan);
        BillingSubscriptionSynchronizer synchronizer =
                new BillingSubscriptionSynchronizer(
                        List.of(mapper), repository, mock(TenantLookupService.class), planService);

        synchronizer.synchronize(event());

        assertThat(subscription.getPlan()).isSameAs(targetPlan);
        assertThat(subscription.getStatus()).isEqualTo(TenantSubscriptionStatus.ACTIVE);
        assertThat(subscription.getBillingProvider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(subscription.getProviderSubscriptionId()).isEqualTo("sub_123");
        assertThat(subscription.getProviderEventCreatedAt()).isEqualTo(update.occurredAt());
        verify(repository).save(subscription);
    }

    @Test
    void ignoresOlderOutOfOrderProviderEvent() {
        UUID tenantId = UUID.randomUUID();
        BillingSubscriptionUpdate update = update(tenantId, Instant.parse("2026-08-22T10:00:00Z"));
        BillingSubscriptionEventMapper mapper = mapper(update);
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription = subscription(tenantId);
        subscription.setProviderEventCreatedAt(Instant.parse("2026-08-22T11:00:00Z"));
        when(repository.findByTenantIdWithPlanForUpdate(tenantId))
                .thenReturn(Optional.of(subscription));
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        BillingSubscriptionSynchronizer synchronizer =
                new BillingSubscriptionSynchronizer(
                        List.of(mapper), repository, mock(TenantLookupService.class), planService);

        synchronizer.synchronize(event());

        verify(planService, never()).getActivePlanEntityByCode("PRO");
        verify(repository, never()).save(subscription);
    }

    private BillingSubscriptionEventMapper mapper(BillingSubscriptionUpdate update) {
        BillingSubscriptionEventMapper mapper = mock(BillingSubscriptionEventMapper.class);
        when(mapper.providerType()).thenReturn(BillingProviderType.STRIPE);
        when(mapper.map(event())).thenReturn(Optional.of(update));
        return mapper;
    }

    private BillingSubscriptionUpdate update(UUID tenantId, Instant occurredAt) {
        return new BillingSubscriptionUpdate(
                BillingProviderType.STRIPE,
                "sub_123",
                tenantId,
                "PRO",
                TenantSubscriptionStatus.ACTIVE,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                null,
                false,
                occurredAt);
    }

    private TenantSubscription subscription(UUID tenantId) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);
        return new TenantSubscription(
                tenant,
                mock(SubscriptionPlan.class),
                TenantSubscriptionStatus.PAST_DUE,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"),
                null,
                false);
    }

    private VerifiedBillingEvent event() {
        return new VerifiedBillingEvent(
                BillingProviderType.STRIPE, "evt_123", "customer.subscription.updated", "{}");
    }
}
