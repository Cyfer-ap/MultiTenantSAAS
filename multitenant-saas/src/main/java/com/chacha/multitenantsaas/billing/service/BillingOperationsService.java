package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingEventOperationsResponse;
import com.chacha.multitenantsaas.billing.dto.BillingSubscriptionOperationsResponse;
import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.BillingEventRepository;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingOperationsService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final BillingEventRepository billingEventRepository;

    public BillingOperationsService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            BillingEventRepository billingEventRepository) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.billingEventRepository = billingEventRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BillingSubscriptionOperationsResponse> getLinkedSubscriptions(
            BillingProviderType provider,
            TenantSubscriptionStatus status,
            String search,
            Pageable pageable) {
        Page<TenantSubscription> subscriptions =
                tenantSubscriptionRepository.findLinkedSubscriptions(
                        provider, status, normalize(search), pageable);

        return toPageResponse(subscriptions.map(this::mapSubscription));
    }

    @Transactional(readOnly = true)
    public PageResponse<BillingEventOperationsResponse> getBillingEvents(
            BillingProviderType provider, String eventType, String search, Pageable pageable) {
        Page<BillingEvent> events =
                billingEventRepository.findBillingEvents(
                        provider, normalize(eventType), normalize(search), pageable);

        return toPageResponse(events.map(this::mapEvent));
    }

    private BillingSubscriptionOperationsResponse mapSubscription(TenantSubscription subscription) {
        return new BillingSubscriptionOperationsResponse(
                subscription.getId(),
                subscription.getTenant().getId(),
                subscription.getTenant().getName(),
                subscription.getPlan().getCode(),
                subscription.getStatus(),
                subscription.getBillingProvider(),
                subscription.getProviderSubscriptionId(),
                subscription.getProviderEventCreatedAt(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getUpdatedAt());
    }

    private BillingEventOperationsResponse mapEvent(BillingEvent event) {
        return new BillingEventOperationsResponse(
                event.getId(),
                event.getProvider(),
                event.getProviderEventId(),
                event.getEventType(),
                event.getReceivedAt());
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private String normalize(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }

        return value.trim();
    }
}
