package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataIntegrityViolationException;

class BillingWebhookServiceTest {

    @Test
    void persistsVerifiedEventBeforeAcknowledgingIt() {
        BillingEventRepository repository = mock(BillingEventRepository.class);
        BillingWebhookService service = new BillingWebhookService(repository);
        VerifiedBillingEvent event = event();

        BillingWebhookReceipt receipt = service.ingest(event);

        ArgumentCaptor<BillingEvent> captor = ArgumentCaptor.forClass(BillingEvent.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(captor.getValue().getProviderEventId()).isEqualTo("evt_123");
        assertThat(captor.getValue().getEventType())
                .isEqualTo("customer.subscription.updated");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"id\":\"evt_123\"}");
        assertThat(receipt.duplicate()).isFalse();
    }

    @Test
    void acknowledgesAlreadyPersistedDeliveryWithoutWritingAgain() {
        BillingEventRepository repository = mock(BillingEventRepository.class);
        when(repository.existsByProviderAndProviderEventId(
                        BillingProviderType.STRIPE, "evt_123"))
                .thenReturn(true);
        BillingWebhookService service = new BillingWebhookService(repository);

        BillingWebhookReceipt receipt = service.ingest(event());

        assertThat(receipt.duplicate()).isTrue();
        verify(repository, never()).saveAndFlush(any(BillingEvent.class));
    }

    @Test
    void treatsConcurrentUniqueConstraintWinnerAsDuplicate() {
        BillingEventRepository repository = mock(BillingEventRepository.class);
        when(repository.existsByProviderAndProviderEventId(
                        BillingProviderType.STRIPE, "evt_123"))
                .thenReturn(false, true);
        when(repository.saveAndFlush(any(BillingEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        BillingWebhookService service = new BillingWebhookService(repository);

        BillingWebhookReceipt receipt = service.ingest(event());

        assertThat(receipt.duplicate()).isTrue();
    }

    private VerifiedBillingEvent event() {
        return new VerifiedBillingEvent(
                BillingProviderType.STRIPE,
                "evt_123",
                "customer.subscription.updated",
                "{\"id\":\"evt_123\"}");
    }
}
