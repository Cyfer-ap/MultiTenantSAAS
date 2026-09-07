package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryAttemptResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryDetailResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.OutboundWebhookDelivery;
import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryAttempt;
import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryStatus;
import com.chacha.multitenantsaas.entity.OutboundWebhookEndpoint;
import com.chacha.multitenantsaas.entity.OutboundWebhookEvent;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.OutboundWebhookDeliveryAttemptRepository;
import com.chacha.multitenantsaas.repository.OutboundWebhookDeliveryRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundWebhookDeliveryManagementService {

    private final OutboundWebhookDeliveryRepository deliveryRepository;
    private final OutboundWebhookDeliveryAttemptRepository attemptRepository;
    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;

    public OutboundWebhookDeliveryManagementService(
            OutboundWebhookDeliveryRepository deliveryRepository,
            OutboundWebhookDeliveryAttemptRepository attemptRepository,
            TenantRepository tenantRepository,
            AuditLogService auditLogService) {
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<OutboundWebhookDeliveryResponse> list(
            UUID tenantId,
            UUID endpointId,
            OutboundWebhookDeliveryStatus status,
            Pageable pageable) {
        requireTenant(tenantId);
        Page<OutboundWebhookDelivery> deliveries;
        if (endpointId != null && status != null) {
            deliveries =
                    deliveryRepository
                            .findAllByTenant_IdAndEndpoint_IdAndStatusOrderByCreatedAtDesc(
                                    tenantId, endpointId, status, pageable);
        } else if (endpointId != null) {
            deliveries =
                    deliveryRepository.findAllByTenant_IdAndEndpoint_IdOrderByCreatedAtDesc(
                            tenantId, endpointId, pageable);
        } else if (status != null) {
            deliveries =
                    deliveryRepository.findAllByTenant_IdAndStatusOrderByCreatedAtDesc(
                            tenantId, status, pageable);
        } else {
            deliveries =
                    deliveryRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId, pageable);
        }

        return new PageResponse<>(
                deliveries.getContent().stream().map(this::mapDelivery).toList(),
                deliveries.getNumber(),
                deliveries.getSize(),
                deliveries.getTotalElements(),
                deliveries.getTotalPages(),
                deliveries.isFirst(),
                deliveries.isLast());
    }

    @Transactional(readOnly = true)
    public OutboundWebhookDeliveryDetailResponse get(UUID tenantId, UUID deliveryId) {
        requireTenant(tenantId);
        OutboundWebhookDelivery delivery = requireDelivery(tenantId, deliveryId);
        return new OutboundWebhookDeliveryDetailResponse(
                mapDelivery(delivery),
                delivery.getEvent().getPayloadJson(),
                attemptRepository.findAllByDelivery_IdOrderByStartedAtAscIdAsc(deliveryId).stream()
                        .map(this::mapAttempt)
                        .toList());
    }

    @Transactional
    public OutboundWebhookDeliveryResponse replay(UUID tenantId, UUID deliveryId, AppUser actor) {
        requireTenantActor(tenantId, actor);
        OutboundWebhookDelivery delivery =
                deliveryRepository
                        .findByIdAndTenantIdForUpdate(deliveryId, tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Outbound webhook delivery not found"));
        OutboundWebhookEndpoint endpoint = delivery.getEndpoint();
        if (endpoint.isArchived()) {
            throw new IllegalArgumentException("Archived webhook endpoints cannot be replayed");
        }
        if (!endpoint.isEnabled()) {
            throw new IllegalArgumentException(
                    "Enable the webhook endpoint before replaying delivery");
        }

        delivery.replay(Instant.now());
        deliveryRepository.save(delivery);
        auditLogService.recordSelfSuccess(
                delivery.getTenant(),
                actor,
                AuditAction.OUTBOUND_WEBHOOK_DELIVERY_REPLAYED,
                "Replayed outbound webhook delivery "
                        + delivery.getId()
                        + " for event "
                        + delivery.getEvent().getId());
        return mapDelivery(delivery);
    }

    private Tenant requireTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required");
        }
        return tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }

    private void requireTenantActor(UUID tenantId, AppUser actor) {
        requireTenant(tenantId);
        if (actor == null
                || actor.getTenant() == null
                || !tenantId.equals(actor.getTenant().getId())) {
            throw new AuthenticationFailedException(
                    "Authenticated user does not belong to this tenant");
        }
    }

    private OutboundWebhookDelivery requireDelivery(UUID tenantId, UUID deliveryId) {
        if (deliveryId == null) {
            throw new IllegalArgumentException("Delivery id is required");
        }
        return deliveryRepository
                .findByIdAndTenant_Id(deliveryId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Outbound webhook delivery not found"));
    }

    private OutboundWebhookDeliveryResponse mapDelivery(OutboundWebhookDelivery delivery) {
        OutboundWebhookEndpoint endpoint = delivery.getEndpoint();
        OutboundWebhookEvent event = delivery.getEvent();
        return new OutboundWebhookDeliveryResponse(
                delivery.getId(),
                endpoint.getId(),
                endpoint.getName(),
                endpoint.getUrl(),
                event.getId(),
                event.getEventType(),
                event.getOccurredAt(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getReplayCount(),
                delivery.getNextAttemptAt(),
                delivery.getLastHttpStatus(),
                delivery.getLastError(),
                delivery.getSentAt(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt());
    }

    private OutboundWebhookDeliveryAttemptResponse mapAttempt(
            OutboundWebhookDeliveryAttempt attempt) {
        return new OutboundWebhookDeliveryAttemptResponse(
                attempt.getId(),
                attempt.getReplayNumber(),
                attempt.getAttemptNumber(),
                attempt.getOutcome(),
                attempt.getHttpStatus(),
                attempt.getError(),
                attempt.getStartedAt(),
                attempt.getCompletedAt());
    }
}
