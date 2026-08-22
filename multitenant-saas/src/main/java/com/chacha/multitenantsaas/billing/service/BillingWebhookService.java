package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingWebhookReceipt;
import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.repository.BillingEventRepository;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingWebhookService {

    private final BillingEventRepository billingEventRepository;
    private final BillingSubscriptionSynchronizer subscriptionSynchronizer;

    public BillingWebhookService(
            BillingEventRepository billingEventRepository,
            BillingSubscriptionSynchronizer subscriptionSynchronizer) {
        this.billingEventRepository = billingEventRepository;
        this.subscriptionSynchronizer = subscriptionSynchronizer;
    }

    @Transactional
    public BillingWebhookReceipt ingest(VerifiedBillingEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Verified billing event is required");
        }

        if (alreadyReceived(event)) {
            return receipt(event, true);
        }

        billingEventRepository.save(
                new BillingEvent(
                        event.provider(),
                        event.providerEventId(),
                        event.eventType(),
                        event.payload()));
        subscriptionSynchronizer.synchronize(event);
        return receipt(event, false);
    }

    private boolean alreadyReceived(VerifiedBillingEvent event) {
        return billingEventRepository.existsByProviderAndProviderEventId(
                event.provider(), event.providerEventId());
    }

    private BillingWebhookReceipt receipt(VerifiedBillingEvent event, boolean duplicate) {
        return new BillingWebhookReceipt(
                event.provider(), event.providerEventId(), event.eventType(), duplicate);
    }
}
