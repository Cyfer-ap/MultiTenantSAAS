package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingWebhookReceipt;
import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.BillingEventRepository;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class BillingWebhookServiceTest {

    @Test
    void persistsThenSynchronizesVerifiedEventInOneTransaction() {
        BillingEventRepository repository = mock(BillingEventRepository.class);
        BillingSubscriptionSynchronizer synchronizer = mock(BillingSubscriptionSynchronizer.class);
        BillingWebhookService service = new BillingWebhookService(repository, synchronizer);
        VerifiedBillingEvent event = event();

        BillingWebhookReceipt receipt = service.ingest(event);

        ArgumentCaptor<BillingEvent> captor = ArgumentCaptor.forClass(BillingEvent.class);
        InOrder order = inOrder(repository, synchronizer);
        order.verify(repository).save(captor.capture());
        order.verify(synchronizer).synchronize(event);
        assertThat(captor.getValue().getProvider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(captor.getValue().getProviderEventId()).isEqualTo("evt_123");
        assertThat(captor.getValue().getEventType()).isEqualTo("customer.subscription.updated");
        assertThat(receipt.duplicate()).isFalse();
    }

    @Test
    void acknowledgesAlreadyPersistedDeliveryWithoutSynchronizingAgain() {
        BillingEventRepository repository = mock(BillingEventRepository.class);
        when(repository.existsByProviderAndProviderEventId(BillingProviderType.STRIPE, "evt_123"))
                .thenReturn(true);
        BillingSubscriptionSynchronizer synchronizer = mock(BillingSubscriptionSynchronizer.class);
        BillingWebhookService service = new BillingWebhookService(repository, synchronizer);

        BillingWebhookReceipt receipt = service.ingest(event());

        assertThat(receipt.duplicate()).isTrue();
        verify(repository, never()).save(any(BillingEvent.class));
        verify(synchronizer, never()).synchronize(any(VerifiedBillingEvent.class));
    }

    private VerifiedBillingEvent event() {
        return new VerifiedBillingEvent(
                BillingProviderType.STRIPE,
                "evt_123",
                "customer.subscription.updated",
                "{\"id\":\"evt_123\"}");
    }
}
