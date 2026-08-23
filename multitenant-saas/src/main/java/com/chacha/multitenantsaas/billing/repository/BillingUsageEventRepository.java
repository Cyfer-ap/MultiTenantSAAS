package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.BillingUsageEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingUsageEventRepository extends JpaRepository<BillingUsageEvent, UUID> {

    Optional<BillingUsageEvent> findByTenant_IdAndIdempotencyKey(
            UUID tenantId, String idempotencyKey);

    @Query(
            """
            SELECT COALESCE(SUM(event.quantity), 0)
            FROM BillingUsageEvent event
            WHERE event.tenant.id = :tenantId
              AND event.metricCode = :metricCode
              AND event.occurredAt >= :periodStart
              AND event.occurredAt < :periodEnd
            """)
    long sumQuantity(
            @Param("tenantId") UUID tenantId,
            @Param("metricCode") String metricCode,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd);

    @Query(
            """
            SELECT COUNT(event)
            FROM BillingUsageEvent event
            WHERE event.tenant.id = :tenantId
              AND event.metricCode = :metricCode
              AND event.occurredAt >= :periodStart
              AND event.occurredAt < :periodEnd
            """)
    long countEvents(
            @Param("tenantId") UUID tenantId,
            @Param("metricCode") String metricCode,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd);
}
