package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.SubscriptionPlanUsageLimitRequest;
import com.chacha.multitenantsaas.billing.dto.SubscriptionPlanUsageLimitResponse;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanUsageLimit;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanUsageLimitRepository;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.SubscriptionPlanRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPlanUsageLimitService {

    private static final Pattern METRIC_CODE_PATTERN =
            Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,63}");

    private final SubscriptionPlanUsageLimitRepository usageLimitRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanUsageLimitService(
            SubscriptionPlanUsageLimitRepository usageLimitRepository,
            SubscriptionPlanRepository subscriptionPlanRepository) {
        this.usageLimitRepository = usageLimitRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public SubscriptionPlanUsageLimitResponse upsert(
            UUID planId, String metricCode, SubscriptionPlanUsageLimitRequest request) {
        if (request == null || request.periodLimit() == null) {
            throw new IllegalArgumentException("Period limit is required");
        }
        if (request.periodLimit() < 0) {
            throw new IllegalArgumentException("Period limit must not be negative");
        }

        SubscriptionPlan plan = requirePlan(planId);
        String normalizedMetricCode = normalizeMetricCode(metricCode);
        SubscriptionPlanUsageLimit usageLimit =
                usageLimitRepository
                        .findByPlan_IdAndMetricCode(planId, normalizedMetricCode)
                        .orElseGet(
                                () ->
                                        new SubscriptionPlanUsageLimit(
                                                plan,
                                                normalizedMetricCode,
                                                request.periodLimit()));

        usageLimit.setPeriodLimit(request.periodLimit());

        return mapResponse(usageLimitRepository.saveAndFlush(usageLimit));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanUsageLimitResponse> list(UUID planId) {
        requirePlan(planId);
        return usageLimitRepository.findAllByPlan_IdOrderByMetricCodeAsc(planId).stream()
                .map(this::mapResponse)
                .toList();
    }

    @Transactional
    public SubscriptionPlanUsageLimitResponse remove(UUID planId, String metricCode) {
        requirePlan(planId);
        String normalizedMetricCode = normalizeMetricCode(metricCode);
        SubscriptionPlanUsageLimit usageLimit =
                usageLimitRepository
                        .findByPlan_IdAndMetricCode(planId, normalizedMetricCode)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Subscription plan usage limit not found"));

        SubscriptionPlanUsageLimitResponse response = mapResponse(usageLimit);
        usageLimitRepository.delete(usageLimit);
        return response;
    }

    private SubscriptionPlan requirePlan(UUID planId) {
        if (planId == null) {
            throw new IllegalArgumentException("Subscription plan id is required");
        }
        return subscriptionPlanRepository
                .findById(planId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Subscription plan not found with id: " + planId));
    }

    private String normalizeMetricCode(String metricCode) {
        if (metricCode == null || !METRIC_CODE_PATTERN.matcher(metricCode.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Metric code must start with a letter and contain only letters, numbers, and underscores");
        }
        return metricCode.trim().toUpperCase(Locale.ROOT);
    }

    private SubscriptionPlanUsageLimitResponse mapResponse(
            SubscriptionPlanUsageLimit usageLimit) {
        return new SubscriptionPlanUsageLimitResponse(
                usageLimit.getId(),
                usageLimit.getPlan().getId(),
                usageLimit.getMetricCode(),
                usageLimit.getPeriodLimit(),
                usageLimit.getCreatedAt(),
                usageLimit.getUpdatedAt());
    }
}
