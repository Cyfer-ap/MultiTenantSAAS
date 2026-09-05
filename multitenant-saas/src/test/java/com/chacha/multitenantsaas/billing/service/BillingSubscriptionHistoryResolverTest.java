package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.BillingEventRepository;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionEventMapper;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingSubscriptionHistoryResolverTest {

    @Test
    void returnsLatestNonTerminalStatePerVerifiedProviderSubscription() {
        UUID tenantId = UUID.randomUUID();
        BillingEventRepository repository = mock(BillingEventRepository.class);
        BillingEvent olderActive =
                event(BillingProviderType.RAZORPAY, "evt_1", "subscription.activated");
        BillingEvent newerCancelled =
                event(BillingProviderType.RAZORPAY, "evt_2", "subscription.cancelled");
        BillingEvent activeCandidate =
                event(BillingProviderType.RAZORPAY, "evt_3", "subscription.activated");
        when(repository.findTop200ByPayloadContainingOrderByReceivedAtDesc(tenantId.toString()))
                .thenReturn(List.of(activeCandidate, newerCancelled, olderActive));

        BillingSubscriptionEventMapper mapper = mock(BillingSubscriptionEventMapper.class);
        when(mapper.providerType()).thenReturn(BillingProviderType.RAZORPAY);
        when(mapper.map(verified(olderActive)))
                .thenReturn(
                        Optional.of(
                                update(
                                        tenantId,
                                        "sub_old",
                                        TenantSubscriptionStatus.ACTIVE,
                                        "2026-08-01T00:00:00Z")));
        when(mapper.map(verified(newerCancelled)))
                .thenReturn(
                        Optional.of(
                                update(
                                        tenantId,
                                        "sub_old",
                                        TenantSubscriptionStatus.CANCELLED,
                                        "2026-08-02T00:00:00Z")));
        when(mapper.map(verified(activeCandidate)))
                .thenReturn(
                        Optional.of(
                                update(
                                        tenantId,
                                        "sub_current",
                                        TenantSubscriptionStatus.ACTIVE,
                                        "2026-08-03T00:00:00Z")));

        BillingSubscriptionHistoryResolver resolver =
                new BillingSubscriptionHistoryResolver(repository, List.of(mapper));

        List<BillingSubscriptionUpdate> candidates = resolver.findNonTerminalCandidates(tenantId);

        assertThat(candidates)
                .extracting(BillingSubscriptionUpdate::providerSubscriptionId)
                .containsExactly("sub_current");
    }

    private BillingEvent event(BillingProviderType provider, String eventId, String eventType) {
        return new BillingEvent(provider, eventId, eventType, "{\"tenant_id\":\"test\"}");
    }

    private VerifiedBillingEvent verified(BillingEvent event) {
        return new VerifiedBillingEvent(
                event.getProvider(),
                event.getProviderEventId(),
                event.getEventType(),
                event.getPayload());
    }

    private BillingSubscriptionUpdate update(
            UUID tenantId,
            String providerSubscriptionId,
            TenantSubscriptionStatus status,
            String occurredAt) {
        return new BillingSubscriptionUpdate(
                BillingProviderType.RAZORPAY,
                providerSubscriptionId,
                tenantId,
                "PRO",
                status,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                null,
                false,
                Instant.parse(occurredAt));
    }
}
