package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OutboundWebhookEndpoint;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboundWebhookEndpointRepository
        extends JpaRepository<OutboundWebhookEndpoint, UUID> {

    List<OutboundWebhookEndpoint> findAllByTenant_IdAndArchivedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId);

    Optional<OutboundWebhookEndpoint> findByIdAndTenant_IdAndArchivedAtIsNull(
            UUID id, UUID tenantId);

    Optional<OutboundWebhookEndpoint> findByIdAndTenant_Id(UUID id, UUID tenantId);

    @Query(
            """
            select distinct endpoint from OutboundWebhookEndpoint endpoint
            join endpoint.eventTypes eventType
            where endpoint.tenant.id = :tenantId
              and endpoint.enabled = true
              and endpoint.archivedAt is null
              and eventType = :eventType
            order by endpoint.createdAt, endpoint.id
            """)
    List<OutboundWebhookEndpoint> findEnabledSubscribers(
            @Param("tenantId") UUID tenantId,
            @Param("eventType") OutboundWebhookEventType eventType);
}
