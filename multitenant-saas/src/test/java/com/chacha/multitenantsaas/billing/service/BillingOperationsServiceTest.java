package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingEventOperationsResponse;
import com.chacha.multitenantsaas.billing.dto.BillingSubscriptionOperationsResponse;
import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.BillingEventRepository;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class BillingOperationsServiceTest {

    @Test
    void returnsProviderLinkageWithoutLoadingProviderCredentials() {
        TenantSubscriptionRepository subscriptionRepository =
                mock(TenantSubscriptionRepository.class);
        BillingEventRepository eventRepository = mock(BillingEventRepository.class);
        BillingOperationsService service =
                new BillingOperationsService(subscriptionRepository, eventRepository);
        Pageable pageable = PageRequest.of(0, 10);

        UUID subscriptionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);
        when(tenant.getName()).thenReturn("Example tenant");
        SubscriptionPlan plan = mock(SubscriptionPlan.class);
        when(plan.getCode()).thenReturn("PRO");

        TenantSubscription subscription = mock(TenantSubscription.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getTenant()).thenReturn(tenant);
        when(subscription.getPlan()).thenReturn(plan);
        when(subscription.getStatus()).thenReturn(TenantSubscriptionStatus.ACTIVE);
        when(subscription.getBillingProvider()).thenReturn(BillingProviderType.RAZORPAY);
        when(subscription.getProviderSubscriptionId()).thenReturn("sub_123");

        when(subscriptionRepository.findLinkedSubscriptions(
                        BillingProviderType.RAZORPAY,
                        TenantSubscriptionStatus.ACTIVE,
                        null,
                        pageable))
                .thenReturn(new PageImpl<>(List.of(subscription), pageable, 1));

        PageResponse<BillingSubscriptionOperationsResponse> response =
                service.getLinkedSubscriptions(
                        BillingProviderType.RAZORPAY,
                        TenantSubscriptionStatus.ACTIVE,
                        "  ",
                        pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        BillingSubscriptionOperationsResponse item = response.content().getFirst();
        assertThat(item.subscriptionId()).isEqualTo(subscriptionId);
        assertThat(item.tenantId()).isEqualTo(tenantId);
        assertThat(item.planCode()).isEqualTo("PRO");
        assertThat(item.provider()).isEqualTo(BillingProviderType.RAZORPAY);
        assertThat(item.providerSubscriptionId()).isEqualTo("sub_123");
    }

    @Test
    void returnsEventMetadataButDoesNotExposeRawPayload() {
        TenantSubscriptionRepository subscriptionRepository =
                mock(TenantSubscriptionRepository.class);
        BillingEventRepository eventRepository = mock(BillingEventRepository.class);
        BillingOperationsService service =
                new BillingOperationsService(subscriptionRepository, eventRepository);
        Pageable pageable = PageRequest.of(0, 10);

        UUID eventId = UUID.randomUUID();
        Instant receivedAt = Instant.parse("2026-08-23T10:15:30Z");
        BillingEvent event = mock(BillingEvent.class);
        when(event.getId()).thenReturn(eventId);
        when(event.getProvider()).thenReturn(BillingProviderType.STRIPE);
        when(event.getProviderEventId()).thenReturn("evt_123");
        when(event.getEventType()).thenReturn("customer.subscription.updated");
        when(event.getReceivedAt()).thenReturn(receivedAt);
        when(eventRepository.findBillingEvents(
                        BillingProviderType.STRIPE,
                        "customer.subscription.updated",
                        "evt_123",
                        pageable))
                .thenReturn(new PageImpl<>(List.of(event), pageable, 1));

        PageResponse<BillingEventOperationsResponse> response =
                service.getBillingEvents(
                        BillingProviderType.STRIPE,
                        "  customer.subscription.updated  ",
                        "  evt_123  ",
                        pageable);

        BillingEventOperationsResponse item = response.content().getFirst();
        assertThat(item.id()).isEqualTo(eventId);
        assertThat(item.providerEventId()).isEqualTo("evt_123");
        assertThat(item.receivedAt()).isEqualTo(receivedAt);
        verify(eventRepository)
                .findBillingEvents(
                        BillingProviderType.STRIPE,
                        "customer.subscription.updated",
                        "evt_123",
                        pageable);
    }
}
