package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantSubscriptionHistoryResponse;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionHistory;
import com.chacha.multitenantsaas.entity.TenantSubscriptionHistoryEventType;
import com.chacha.multitenantsaas.repository.TenantSubscriptionHistoryRepository;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSubscriptionHistoryService {

    private final TenantSubscriptionHistoryRepository historyRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantLookupService tenantLookupService;

    public TenantSubscriptionHistoryService(
            TenantSubscriptionHistoryRepository historyRepository,
            TenantSubscriptionRepository subscriptionRepository,
            TenantLookupService tenantLookupService) {
        this.historyRepository = historyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantLookupService = tenantLookupService;
    }

    @Transactional
    public void ensureBaseline(TenantSubscription subscription) {
        Objects.requireNonNull(subscription, "subscription must not be null");
        if (subscription.getId() == null) {
            return;
        }
        if (!historyRepository.existsBySubscriptionId(subscription.getId())) {
            record(subscription, TenantSubscriptionHistoryEventType.MIGRATED_CURRENT_STATE);
        }
    }

    @Transactional
    public void record(
            TenantSubscription subscription, TenantSubscriptionHistoryEventType eventType) {
        Objects.requireNonNull(subscription, "subscription must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        if (subscription.getId() == null) {
            throw new IllegalStateException("Subscription must be persisted before history is recorded.");
        }
        historyRepository.save(new TenantSubscriptionHistory(subscription, eventType));
    }

    @Transactional
    public Page<TenantSubscriptionHistoryResponse> getHistory(UUID tenantId, Pageable pageable) {
        tenantLookupService.ensureExists(tenantId);
        subscriptionRepository.findByTenantIdWithPlan(tenantId).ifPresent(this::ensureBaseline);
        return historyRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    private TenantSubscriptionHistoryResponse toResponse(TenantSubscriptionHistory history) {
        return new TenantSubscriptionHistoryResponse(
                history.getId(),
                history.getSubscriptionId(),
                history.getTenantId(),
                history.getTenantNameSnapshot(),
                history.getPlanId(),
                history.getPlanCodeSnapshot(),
                history.getPlanNameSnapshot(),
                history.getPlanDescriptionSnapshot(),
                history.getBillingIntervalSnapshot(),
                history.getPriceSnapshot(),
                history.getCurrencySnapshot(),
                history.getMaxUsersSnapshot(),
                history.getMaxProjectsSnapshot(),
                history.getMaxStorageMbSnapshot(),
                history.getStatus(),
                history.getStartedAt(),
                history.getCurrentPeriodStart(),
                history.getCurrentPeriodEnd(),
                history.getTrialEndsAt(),
                history.isCancelAtPeriodEnd(),
                history.getCancelledAt(),
                history.getBillingProvider(),
                history.getProviderSubscriptionId(),
                history.getProviderEventCreatedAt(),
                history.getEventType(),
                history.getRecordedAt());
    }
}
