package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OutboundWebhookSubscriptionPayload;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionLifecycleUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionPlanChangeRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionHistoryEventType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSubscriptionService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantLookupService tenantLookupService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final TenantSubscriptionHistoryService historyService;
    private final OutboundWebhookEventService outboundWebhookEventService;

    public TenantSubscriptionService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            TenantLookupService tenantLookupService,
            SubscriptionPlanService subscriptionPlanService) {
        this(
                tenantSubscriptionRepository,
                tenantLookupService,
                subscriptionPlanService,
                null,
                null);
    }

    public TenantSubscriptionService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            TenantLookupService tenantLookupService,
            SubscriptionPlanService subscriptionPlanService,
            TenantSubscriptionHistoryService historyService) {
        this(
                tenantSubscriptionRepository,
                tenantLookupService,
                subscriptionPlanService,
                historyService,
                null);
    }

    @Autowired
    public TenantSubscriptionService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            TenantLookupService tenantLookupService,
            SubscriptionPlanService subscriptionPlanService,
            TenantSubscriptionHistoryService historyService,
            OutboundWebhookEventService outboundWebhookEventService) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.tenantLookupService = tenantLookupService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.historyService = historyService;
        this.outboundWebhookEventService = outboundWebhookEventService;
    }

    @Transactional
    public TenantSubscriptionResponse startSubscription(
            UUID tenantId, TenantSubscriptionStartRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Tenant subscription request is required.");
        }

        Tenant tenant = tenantLookupService.getActiveByIdForUpdateOrThrow(tenantId);

        if (tenantSubscriptionRepository.existsByTenant_Id(tenantId)) {
            throw new DuplicateResourceException("Tenant already has a subscription.");
        }

        SubscriptionPlan plan = subscriptionPlanService.getActivePlanEntity(request.planId());

        TenantSubscriptionStatus status = requireValue(request.status(), "Subscription status");

        if (status != TenantSubscriptionStatus.TRIALING
                && status != TenantSubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "A new subscription must start as " + "TRIALING or ACTIVE.");
        }

        Instant startedAt = request.startedAt() == null ? Instant.now() : request.startedAt();

        Instant periodStart =
                request.currentPeriodStart() == null ? startedAt : request.currentPeriodStart();

        validatePeriod(periodStart, request.currentPeriodEnd(), request.trialEndsAt(), status);

        TenantSubscription subscription =
                new TenantSubscription(
                        tenant,
                        plan,
                        status,
                        startedAt,
                        periodStart,
                        request.currentPeriodEnd(),
                        request.trialEndsAt(),
                        request.cancelAtPeriodEnd());

        TenantSubscription saved = tenantSubscriptionRepository.saveAndFlush(subscription);
        record(saved, TenantSubscriptionHistoryEventType.STARTED);
        publish(saved, OutboundWebhookEventType.SUBSCRIPTION_UPDATED);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TenantSubscriptionResponse getSubscription(UUID tenantId) {
        tenantLookupService.ensureExists(tenantId);
        return mapToResponse(getSubscriptionEntity(tenantId));
    }

    @Transactional
    public TenantSubscriptionResponse changePlan(
            UUID tenantId, TenantSubscriptionPlanChangeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Plan-change request is required.");
        }

        TenantSubscription subscription = getSubscriptionEntityForUpdate(tenantId);
        ensureBaseline(subscription);

        if (subscription.getStatus() == TenantSubscriptionStatus.CANCELLED
                || subscription.getStatus() == TenantSubscriptionStatus.EXPIRED) {
            throw new IllegalArgumentException(
                    "Cancelled or expired subscriptions " + "cannot change plans.");
        }

        SubscriptionPlan plan = subscriptionPlanService.getActivePlanEntity(request.planId());

        validatePeriod(
                request.currentPeriodStart(),
                request.currentPeriodEnd(),
                null,
                TenantSubscriptionStatus.ACTIVE);

        subscription.setPlan(plan);
        subscription.setCurrentPeriodStart(request.currentPeriodStart());
        subscription.setCurrentPeriodEnd(request.currentPeriodEnd());
        subscription.setTrialEndsAt(null);

        TenantSubscription saved = tenantSubscriptionRepository.saveAndFlush(subscription);
        record(saved, TenantSubscriptionHistoryEventType.PLAN_CHANGED);
        publish(saved, OutboundWebhookEventType.SUBSCRIPTION_UPDATED);
        return mapToResponse(saved);
    }

    @Transactional
    public TenantSubscriptionResponse updateLifecycle(
            UUID tenantId, TenantSubscriptionLifecycleUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Subscription lifecycle request is required.");
        }

        TenantSubscription subscription = getSubscriptionEntityForUpdate(tenantId);
        ensureBaseline(subscription);

        TenantSubscriptionStatus status = requireValue(request.status(), "Subscription status");

        Instant periodEnd =
                request.currentPeriodEnd() == null
                        ? subscription.getCurrentPeriodEnd()
                        : request.currentPeriodEnd();

        validatePeriod(
                subscription.getCurrentPeriodStart(), periodEnd, request.trialEndsAt(), status);

        subscription.setStatus(status);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setTrialEndsAt(request.trialEndsAt());
        subscription.setCancelAtPeriodEnd(request.cancelAtPeriodEnd());

        if (status == TenantSubscriptionStatus.CANCELLED) {
            subscription.setCancelledAt(Instant.now());
            subscription.setCancelAtPeriodEnd(false);
        } else if (status == TenantSubscriptionStatus.ACTIVE
                || status == TenantSubscriptionStatus.TRIALING) {
            subscription.setCancelledAt(null);
        }

        TenantSubscription saved = tenantSubscriptionRepository.saveAndFlush(subscription);
        record(saved, TenantSubscriptionHistoryEventType.LIFECYCLE_UPDATED);
        publish(
                saved,
                status == TenantSubscriptionStatus.CANCELLED
                        ? OutboundWebhookEventType.SUBSCRIPTION_CANCELLED
                        : OutboundWebhookEventType.SUBSCRIPTION_UPDATED);
        return mapToResponse(saved);
    }

    private TenantSubscription getSubscriptionEntity(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required.");
        }

        return tenantSubscriptionRepository
                .findByTenantIdWithPlan(tenantId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant subscription not found for tenant: " + tenantId));
    }

    private TenantSubscription getSubscriptionEntityForUpdate(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required.");
        }

        return tenantSubscriptionRepository
                .findByTenantIdWithPlanForUpdate(tenantId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant subscription not found for tenant: " + tenantId));
    }

    private void validatePeriod(
            Instant periodStart,
            Instant periodEnd,
            Instant trialEndsAt,
            TenantSubscriptionStatus status) {
        if (periodStart == null) {
            throw new IllegalArgumentException("Current period start is required.");
        }

        if (periodEnd == null) {
            throw new IllegalArgumentException("Current period end is required.");
        }

        if (!periodEnd.isAfter(periodStart)) {
            throw new IllegalArgumentException(
                    "Current period end must be after " + "the period start.");
        }

        if (status == TenantSubscriptionStatus.TRIALING) {
            if (trialEndsAt == null) {
                throw new IllegalArgumentException(
                        "Trial end is required for a " + "trialing subscription.");
            }

            if (trialEndsAt.isBefore(periodStart) || trialEndsAt.isAfter(periodEnd)) {
                throw new IllegalArgumentException(
                        "Trial end must be inside the " + "current billing period.");
            }
        }
    }

    private TenantSubscriptionResponse mapToResponse(TenantSubscription subscription) {
        SubscriptionPlan plan = subscription.getPlan();
        boolean hasPlanSnapshot = subscription.getPlanCodeSnapshot() != null;

        SubscriptionPlanResponse planResponse =
                new SubscriptionPlanResponse(
                        plan.getId(),
                        hasPlanSnapshot ? subscription.getPlanCodeSnapshot() : plan.getCode(),
                        hasPlanSnapshot ? subscription.getPlanNameSnapshot() : plan.getName(),
                        hasPlanSnapshot
                                ? subscription.getPlanDescriptionSnapshot()
                                : plan.getDescription(),
                        hasPlanSnapshot
                                ? subscription.getBillingIntervalSnapshot()
                                : plan.getBillingInterval(),
                        hasPlanSnapshot ? subscription.getPriceSnapshot() : plan.getPrice(),
                        hasPlanSnapshot ? subscription.getCurrencySnapshot() : plan.getCurrency(),
                        hasPlanSnapshot ? subscription.getMaxUsersSnapshot() : plan.getMaxUsers(),
                        hasPlanSnapshot
                                ? subscription.getMaxProjectsSnapshot()
                                : plan.getMaxProjects(),
                        hasPlanSnapshot
                                ? subscription.getMaxStorageMbSnapshot()
                                : plan.getMaxStorageMb(),
                        plan.getStatus(),
                        plan.getCreatedAt(),
                        plan.getUpdatedAt());

        return new TenantSubscriptionResponse(
                subscription.getId(),
                subscription.getTenant().getId(),
                subscription.getTenant().getName(),
                planResponse,
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getTrialEndsAt(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCancelledAt(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }

    private void ensureBaseline(TenantSubscription subscription) {
        if (historyService != null) {
            historyService.ensureBaseline(subscription);
        }
    }

    private void record(
            TenantSubscription subscription, TenantSubscriptionHistoryEventType eventType) {
        if (historyService != null) {
            historyService.record(subscription, eventType);
        }
    }

    private void publish(TenantSubscription subscription, OutboundWebhookEventType eventType) {
        if (outboundWebhookEventService != null) {
            outboundWebhookEventService.publish(
                    subscription.getTenant().getId(),
                    eventType,
                    OutboundWebhookSubscriptionPayload.from(subscription));
        }
    }

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return value;
    }
}
