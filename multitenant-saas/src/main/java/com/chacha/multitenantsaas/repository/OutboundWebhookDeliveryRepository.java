package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OutboundWebhookDelivery;
import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboundWebhookDeliveryRepository
        extends JpaRepository<OutboundWebhookDelivery, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select delivery from OutboundWebhookDelivery delivery
            join fetch delivery.tenant
            join fetch delivery.endpoint
            join fetch delivery.event
            where (delivery.status in :readyStatuses and delivery.nextAttemptAt <= :now)
               or (delivery.status = :processingStatus
                   and delivery.processingStartedAt <= :staleBefore)
            order by delivery.createdAt, delivery.id
            """)
    List<OutboundWebhookDelivery> findClaimableForUpdate(
            @Param("readyStatuses") Collection<OutboundWebhookDeliveryStatus> readyStatuses,
            @Param("processingStatus") OutboundWebhookDeliveryStatus processingStatus,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select delivery from OutboundWebhookDelivery delivery where delivery.id = :deliveryId")
    Optional<OutboundWebhookDelivery> findByIdForUpdate(@Param("deliveryId") UUID deliveryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select delivery from OutboundWebhookDelivery delivery
            join fetch delivery.tenant
            join fetch delivery.endpoint
            join fetch delivery.event
            where delivery.id = :deliveryId and delivery.tenant.id = :tenantId
            """)
    Optional<OutboundWebhookDelivery> findByIdAndTenantIdForUpdate(
            @Param("deliveryId") UUID deliveryId, @Param("tenantId") UUID tenantId);

    Optional<OutboundWebhookDelivery> findByIdAndTenant_Id(UUID deliveryId, UUID tenantId);

    Page<OutboundWebhookDelivery> findAllByTenant_IdOrderByCreatedAtDesc(
            UUID tenantId, Pageable pageable);

    Page<OutboundWebhookDelivery> findAllByTenant_IdAndEndpoint_IdOrderByCreatedAtDesc(
            UUID tenantId, UUID endpointId, Pageable pageable);

    Page<OutboundWebhookDelivery> findAllByTenant_IdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, OutboundWebhookDeliveryStatus status, Pageable pageable);

    Page<OutboundWebhookDelivery> findAllByTenant_IdAndEndpoint_IdAndStatusOrderByCreatedAtDesc(
            UUID tenantId,
            UUID endpointId,
            OutboundWebhookDeliveryStatus status,
            Pageable pageable);
}
