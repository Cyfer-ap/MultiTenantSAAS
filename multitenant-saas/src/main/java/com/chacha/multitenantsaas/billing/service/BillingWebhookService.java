package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingWebhookReceipt;
import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.repository.BillingEventRepository;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class BillingWebhookService {

    private final BillingEventRepository billingEventRepository;

    public BillingWebhookService(BillingEventRepository billingEventRepository) {
        this.billingEventRepository = billingEventRepository;
    }

    public BillingWebhookReceipt ingest(VerifiedBillingEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Verified billing event is required");
        }

        if (alreadyReceived(event)) {
            return receipt(event, true);
        }

        try {
            billingEventRepository.saveAndFlush(
                    new BillingEvent(
                            event.provider(),
                            event.providerEventId(),
                            event.eventType(),
                            event.payload()));
            return receipt(event, false);
        } catch (DataIntegrityViolationException exception) {
            if (alreadyReceived(event)) {
                return receipt(event, true);
            }
            throw exception;
        }
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
