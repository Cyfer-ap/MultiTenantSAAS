package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingUsageRecordRequest;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanUsageLimit;
import com.chacha.multitenantsaas.billing.repository.BillingUsageEventRepository;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanUsageLimitRepository;
import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.exception.ApiUsageLimitExceededException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.SubscriptionEntitlementService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiRequestQuotaService {

    public static final String API_REQUESTS_METRIC = "API_REQUESTS";

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final SubscriptionEntitlementService subscriptionEntitlementService;
    private final SubscriptionPlanUsageLimitRepository usageLimitRepository;
    private final BillingUsageEventRepository usageEventRepository;
    private final BillingUsageMeteringService usageMeteringService;

    public ApiRequestQuotaService(
            TenantSubscriptionRepository tenantSubscriptionRepository,
            SubscriptionEntitlementService subscriptionEntitlementService,
            SubscriptionPlanUsageLimitRepository usageLimitRepository,
            BillingUsageEventRepository usageEventRepository,
            BillingUsageMeteringService usageMeteringService) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.subscriptionEntitlementService = subscriptionEntitlementService;
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

        TenantSubscriptionEntitlementResponse entitlement =
                subscriptionEntitlementService.evaluate(tenantId);
        if (!entitlement.serviceAvailable()) {
            throw serviceUnavailable(entitlement.accessReason());
        }

        Instant occurredAt = Instant.now();
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
