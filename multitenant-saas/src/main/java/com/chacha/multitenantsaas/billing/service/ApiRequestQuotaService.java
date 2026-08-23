package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingUsageRecordRequest;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanUsageLimit;
import com.chacha.multitenantsaas.billing.repository.BillingUsageEventRepository;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanUsageLimitRepository;
import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.ApiUsageLimitExceededException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiRequestQuotaService {

    public static final String API_REQUESTS_METRIC = "API_REQUESTS";

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final SubscriptionPlanUsageLimitRepository usageLimitRepository;
    private final BillingUsageEventRepository usageEventRepository;
    private final BillingUsageMeteringService usageMeteringService;

    public ApiRequestQuotaService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            SubscriptionPlanUsageLimitRepository usageLimitRepository,
            BillingUsageEventRepository usageEventRepository,
            BillingUsageMeteringService usageMeteringService) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.usageLimitRepository = usageLimitRepository;
        this.usageEventRepository = usageEventRepository;
        this.usageMeteringService = usageMeteringService;
    }

    @Transactional
    public void consume(UUID tenantId, UUID apiKeyId) {
        TenantSubscription subscription =
                tenantSubscriptionRepository
                        .findByTenantIdWithPlanForUpdate(tenantId)
                        .orElseThrow(
                                () ->
                                        serviceUnavailable(
                                                SubscriptionAccessReason.NO_SUBSCRIPTION));

        Instant occurredAt = Instant.now();
        SubscriptionAccessReason unavailableReason =
                resolveUnavailableReason(subscription, occurredAt);
        if (unavailableReason != null) {
            throw serviceUnavailable(unavailableReason);
        }
        Instant periodStart = subscription.getCurrentPeriodStart();
        Instant periodEnd = subscription.getCurrentPeriodEnd();
        if (periodStart == null
                || periodEnd == null
                || occurredAt.isBefore(periodStart)
                || !occurredAt.isBefore(periodEnd)) {
            throw serviceUnavailable(SubscriptionAccessReason.PERIOD_EXPIRED);
        }

        SubscriptionPlanUsageLimit usageLimit =
                usageLimitRepository
                        .findByPlan_IdAndMetricCode(
                                subscription.getPlan().getId(), API_REQUESTS_METRIC)
                        .orElse(null);
        if (usageLimit != null) {
            long used =
                    usageEventRepository.sumQuantity(
                            tenantId, API_REQUESTS_METRIC, periodStart, periodEnd);
            if (used >= usageLimit.getPeriodLimit()) {
                throw new ApiUsageLimitExceededException(
                        API_REQUESTS_METRIC,
                        used,
                        usageLimit.getPeriodLimit(),
                        periodEnd);
            }
        }

        usageMeteringService.recordUsage(
                new BillingUsageRecordRequest(
                        tenantId,
                        API_REQUESTS_METRIC,
                        1L,
                        "api:" + apiKeyId + ":" + UUID.randomUUID(),
                        occurredAt));
    }

    private SubscriptionAccessReason resolveUnavailableReason(
            TenantSubscription subscription, Instant evaluatedAt) {
        if (subscription.getPlan().getStatus() != SubscriptionPlanStatus.ACTIVE) {
            return SubscriptionAccessReason.PLAN_INACTIVE;
        }
        if (subscription.getStatus() == TenantSubscriptionStatus.CANCELLED) {
            return SubscriptionAccessReason.CANCELLED;
        }
        if (subscription.getStatus() == TenantSubscriptionStatus.EXPIRED) {
            return SubscriptionAccessReason.EXPIRED;
        }
        if (subscription.getCurrentPeriodEnd() == null
                || !subscription.getCurrentPeriodEnd().isAfter(evaluatedAt)) {
            return SubscriptionAccessReason.PERIOD_EXPIRED;
        }
        if (subscription.getStatus() == TenantSubscriptionStatus.TRIALING
                && (subscription.getTrialEndsAt() == null
                        || !subscription.getTrialEndsAt().isAfter(evaluatedAt))) {
            return SubscriptionAccessReason.TRIAL_EXPIRED;
        }
        return null;
    }

    private SubscriptionRestrictionException serviceUnavailable(
            SubscriptionAccessReason accessReason) {
        return new SubscriptionRestrictionException(
                RestrictionType.SERVICE_UNAVAILABLE,
                accessReason,
                "api_requests",
                null,
                null,
                "API access is unavailable for the current subscription");
    }
}
