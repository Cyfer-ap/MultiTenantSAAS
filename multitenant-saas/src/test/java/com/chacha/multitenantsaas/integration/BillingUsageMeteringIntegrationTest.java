package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.chacha.multitenantsaas.billing.dto.BillingUsageRecordRequest;
import com.chacha.multitenantsaas.billing.dto.BillingUsageRecordResponse;
import com.chacha.multitenantsaas.billing.dto.BillingUsageSummaryResponse;
import com.chacha.multitenantsaas.billing.service.BillingUsageMeteringService;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BillingUsageMeteringIntegrationTest {

    @Autowired private BillingUsageMeteringService meteringService;

    @Autowired private TenantRepository tenantRepository;

    @Test
    void recordsIdempotentlyAndAggregatesUsingHalfOpenBillingPeriod() {
        Tenant tenant = createTenant();
        Instant periodStart = Instant.parse("2026-08-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-09-01T00:00:00Z");

        BillingUsageRecordResponse first =
                meteringService.recordUsage(
                        request(tenant.getId(), "api_requests", 1L, "request-1", periodStart));
        meteringService.recordUsage(
                request(
                        tenant.getId(),
                        "API_REQUESTS",
                        2L,
                        "request-2",
                        periodEnd.minusSeconds(1)));
        meteringService.recordUsage(
                request(tenant.getId(), "API_REQUESTS", 8L, "request-3", periodEnd));
        BillingUsageRecordResponse duplicate =
                meteringService.recordUsage(
                        request(tenant.getId(), "api_requests", 1L, "request-1", periodStart));

        BillingUsageSummaryResponse summary =
                meteringService.summarize(
                        tenant.getId(), "api_requests", periodStart, periodEnd);

        assertThat(first.duplicate()).isFalse();
        assertThat(first.metricCode()).isEqualTo("API_REQUESTS");
        assertThat(first.recordedAt()).isNotNull();
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(duplicate.eventId()).isEqualTo(first.eventId());
        assertThat(summary.quantity()).isEqualTo(3L);
        assertThat(summary.eventCount()).isEqualTo(2L);
    }

    @Test
    void rejectsReuseOfIdempotencyKeyForDifferentUsageData() {
        Tenant tenant = createTenant();
        Instant occurredAt = Instant.parse("2026-08-23T00:00:00Z");
        meteringService.recordUsage(
                request(tenant.getId(), "API_REQUESTS", 1L, "request-1", occurredAt));

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                meteringService.recordUsage(
                                        request(
                                                tenant.getId(),
                                                "API_REQUESTS",
                                                2L,
                                                "request-1",
                                                occurredAt)))
                .withMessage("Idempotency key is already used for different usage data");
    }

    private BillingUsageRecordRequest request(
            UUID tenantId,
            String metricCode,
            long quantity,
            String idempotencyKey,
            Instant occurredAt) {
        return new BillingUsageRecordRequest(
                tenantId, metricCode, quantity, idempotencyKey, occurredAt);
    }

    private Tenant createTenant() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return tenantRepository.saveAndFlush(
                new Tenant("Usage Labs", "usage-labs-" + suffix));
    }
}
