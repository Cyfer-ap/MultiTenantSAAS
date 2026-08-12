package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionLifecycleUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionPlanChangeRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSubscriptionService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;

    private final TenantLookupService tenantLookupService;

    private final SubscriptionPlanService subscriptionPlanService;

    public TenantSubscriptionService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            TenantLookupService tenantLookupService,
            SubscriptionPlanService subscriptionPlanService) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.tenantLookupService = tenantLookupService;
        this.subscriptionPlanService = subscriptionPlanService;
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

        return mapToResponse(tenantSubscriptionRepository.saveAndFlush(subscription));
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

        return mapToResponse(tenantSubscriptionRepository.saveAndFlush(subscription));
    }

    @Transactional
    public TenantSubscriptionResponse updateLifecycle(
            UUID tenantId, TenantSubscriptionLifecycleUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Subscription lifecycle request is required.");
        }

        TenantSubscription subscription = getSubscriptionEntityForUpdate(tenantId);

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

        return mapToResponse(tenantSubscriptionRepository.saveAndFlush(subscription));
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

        SubscriptionPlanResponse planResponse =
                new SubscriptionPlanResponse(
                        plan.getId(),
                        plan.getCode(),
                        plan.getName(),
                        plan.getDescription(),
                        plan.getBillingInterval(),
                        plan.getPrice(),
                        plan.getCurrency(),
                        plan.getMaxUsers(),
                        plan.getMaxProjects(),
                        plan.getMaxStorageMb(),
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

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return value;
    }
}
