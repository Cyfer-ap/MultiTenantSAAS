package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.SubscriptionPlanUpdateRequest;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.SubscriptionPlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public SubscriptionPlanResponse createPlan(SubscriptionPlanCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Subscription plan request is required.");
        }

        String code = normalizeCode(request.code());

        if (subscriptionPlanRepository.existsByCode(code)) {
            throw new DuplicateResourceException(
                    "Subscription plan already exists with code: " + code);
        }

        SubscriptionPlan plan =
                new SubscriptionPlan(
                        code,
                        normalizeRequiredText(request.name(), "Plan name", 150),
                        normalizeOptionalText(request.description(), "Plan description", 500),
                        requireValue(request.billingInterval(), "Billing interval"),
                        normalizePrice(request.price()),
                        normalizeCurrency(request.currency()),
                        normalizeLimit(request.maxUsers(), "Maximum users"),
                        normalizeLimit(request.maxProjects(), "Maximum projects"),
                        normalizeLimit(request.maxStorageMb(), "Maximum storage"));

        return mapToResponse(subscriptionPlanRepository.saveAndFlush(plan));
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(UUID planId, SubscriptionPlanUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Subscription plan request is required.");
        }

        SubscriptionPlan plan = getPlanEntity(planId);

        plan.setName(normalizeRequiredText(request.name(), "Plan name", 150));
        plan.setDescription(normalizeOptionalText(request.description(), "Plan description", 500));
        plan.setBillingInterval(requireValue(request.billingInterval(), "Billing interval"));
        plan.setPrice(normalizePrice(request.price()));
        plan.setCurrency(normalizeCurrency(request.currency()));
        plan.setMaxUsers(normalizeLimit(request.maxUsers(), "Maximum users"));
        plan.setMaxProjects(normalizeLimit(request.maxProjects(), "Maximum projects"));
        plan.setMaxStorageMb(normalizeLimit(request.maxStorageMb(), "Maximum storage"));

        return mapToResponse(subscriptionPlanRepository.saveAndFlush(plan));
    }

    @Transactional
    public SubscriptionPlanResponse changeStatus(UUID planId, SubscriptionPlanStatus status) {
        SubscriptionPlan plan = getPlanEntity(planId);

        plan.setStatus(requireValue(status, "Subscription plan status"));

        return mapToResponse(subscriptionPlanRepository.saveAndFlush(plan));
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlan(UUID planId) {
        return mapToResponse(getPlanEntity(planId));
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanByCode(String code) {
        String normalizedCode = normalizeCode(code);

        return mapToResponse(
                subscriptionPlanRepository
                        .findByCode(normalizedCode)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Subscription plan not found with code: "
                                                        + normalizedCode)));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPlans(boolean activeOnly) {
        List<SubscriptionPlan> plans =
                activeOnly
                        ? subscriptionPlanRepository.findByStatusOrderByPriceAscCodeAsc(
                                SubscriptionPlanStatus.ACTIVE)
                        : subscriptionPlanRepository.findAllByOrderByPriceAscCodeAsc();

        return plans.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getActivePlanEntity(UUID planId) {
        SubscriptionPlan plan = getPlanEntity(planId);

        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Subscription plan must be active.");
        }

        return plan;
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getActivePlanEntityByCode(String code) {
        String normalizedCode = normalizeCode(code);
        SubscriptionPlan plan =
                subscriptionPlanRepository
                        .findByCode(normalizedCode)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Subscription plan not found with code: "
                                                        + normalizedCode));

        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Subscription plan must be active.");
        }
        return plan;
    }

    private SubscriptionPlan getPlanEntity(UUID planId) {
        if (planId == null) {
            throw new IllegalArgumentException("Subscription plan id is required.");
        }

        return subscriptionPlanRepository
                .findById(planId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Subscription plan not found with id: " + planId));
    }

    private SubscriptionPlanResponse mapToResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
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
    }

    private String normalizeCode(String value) {
        String normalized =
                normalizeRequiredText(value, "Plan code", 60)
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_');

        if (!normalized.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Plan code may contain letters, numbers, " + "underscores, and hyphens only.");
        }

        return normalized;
    }

    private String normalizeCurrency(String value) {
        String currency = normalizeRequiredText(value, "Currency", 3).toUpperCase(Locale.ROOT);

        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter ISO code.");
        }

        return currency;
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Plan price is required.");
        }

        if (value.signum() < 0) {
            throw new IllegalArgumentException("Plan price must not be negative.");
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer normalizeLimit(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative.");
        }

        return value;
    }

    private Long normalizeLimit(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative.");
        }

        return value;
    }

    private String normalizeRequiredText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters.");
        }

        return normalized;
    }

    private String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters.");
        }

        return normalized;
    }

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return value;
    }
}
