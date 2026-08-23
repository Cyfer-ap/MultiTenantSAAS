package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.dto.BillingUsageRecordRequest;
import com.chacha.multitenantsaas.billing.dto.BillingUsageRecordResponse;
import com.chacha.multitenantsaas.billing.dto.BillingUsageSummaryResponse;
import com.chacha.multitenantsaas.billing.entity.BillingUsageEvent;
import com.chacha.multitenantsaas.billing.repository.BillingUsageEventRepository;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class BillingUsageMeteringService {

    private static final Pattern METRIC_CODE_PATTERN =
            Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,63}");

    private final BillingUsageEventRepository usageEventRepository;
    private final TenantRepository tenantRepository;

    public BillingUsageMeteringService(
            BillingUsageEventRepository usageEventRepository, TenantRepository tenantRepository) {
        this.usageEventRepository = usageEventRepository;
        this.tenantRepository = tenantRepository;
    }

    public BillingUsageRecordResponse recordUsage(BillingUsageRecordRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        UUID tenantId = Objects.requireNonNull(request.tenantId(), "tenantId must not be null");
        String metricCode = normalizeMetricCode(request.metricCode());
        long quantity = requirePositiveQuantity(request.quantity());
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        Instant occurredAt =
                Objects.requireNonNull(request.occurredAt(), "occurredAt must not be null");

        BillingUsageEvent existing =
                usageEventRepository
                        .findByTenant_IdAndIdempotencyKey(tenantId, idempotencyKey)
                        .orElse(null);
        if (existing != null) {
            return resolveDuplicate(
                    existing, tenantId, metricCode, quantity, idempotencyKey, occurredAt);
        }

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant not found: " + tenantId));
        BillingUsageEvent event =
                new BillingUsageEvent(tenant, metricCode, quantity, idempotencyKey, occurredAt);

        try {
            BillingUsageEvent saved = usageEventRepository.saveAndFlush(event);
            return mapRecord(saved, tenantId, false);
        } catch (DataIntegrityViolationException exception) {
            BillingUsageEvent concurrent =
                    usageEventRepository
                            .findByTenant_IdAndIdempotencyKey(tenantId, idempotencyKey)
                            .orElseThrow(() -> exception);
            return resolveDuplicate(
                    concurrent, tenantId, metricCode, quantity, idempotencyKey, occurredAt);
        }
    }

    public BillingUsageSummaryResponse summarize(
            UUID tenantId, String metricCode, Instant periodStart, Instant periodEnd) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        String normalizedMetricCode = normalizeMetricCode(metricCode);
        Objects.requireNonNull(periodStart, "periodStart must not be null");
        Objects.requireNonNull(periodEnd, "periodEnd must not be null");
        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException("periodStart must be before periodEnd");
        }
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found: " + tenantId);
        }

        long quantity =
                usageEventRepository.sumQuantity(
                        tenantId, normalizedMetricCode, periodStart, periodEnd);
        long eventCount =
                usageEventRepository.countEvents(
                        tenantId, normalizedMetricCode, periodStart, periodEnd);

        return new BillingUsageSummaryResponse(
                tenantId, normalizedMetricCode, periodStart, periodEnd, quantity, eventCount);
    }

    private BillingUsageRecordResponse resolveDuplicate(
            BillingUsageEvent existing,
            UUID tenantId,
            String metricCode,
            long quantity,
            String idempotencyKey,
            Instant occurredAt) {
        if (!existing.getMetricCode().equals(metricCode)
                || existing.getQuantity() != quantity
                || !existing.getOccurredAt().equals(occurredAt)) {
            throw new IllegalArgumentException(
                    "Idempotency key is already used for different usage data");
        }
        return mapRecord(existing, tenantId, true);
    }

    private BillingUsageRecordResponse mapRecord(
            BillingUsageEvent event, UUID tenantId, boolean duplicate) {
        return new BillingUsageRecordResponse(
                event.getId(),
                tenantId,
                event.getMetricCode(),
                event.getQuantity(),
                event.getIdempotencyKey(),
                event.getOccurredAt(),
                event.getRecordedAt(),
                duplicate);
    }

    private String normalizeMetricCode(String metricCode) {
        if (metricCode == null || !METRIC_CODE_PATTERN.matcher(metricCode.trim()).matches()) {
            throw new IllegalArgumentException(
                    "metricCode must start with a letter and contain only letters, numbers, and underscores");
        }
        return metricCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("idempotencyKey must not exceed 128 characters");
        }
        return normalized;
    }

    private long requirePositiveQuantity(Long quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return quantity;
    }
}
