package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.OutboundWebhookDelivery;
import com.chacha.multitenantsaas.entity.OutboundWebhookEndpoint;
import com.chacha.multitenantsaas.entity.OutboundWebhookEvent;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.OutboundWebhookDeliveryRepository;
import com.chacha.multitenantsaas.repository.OutboundWebhookEndpointRepository;
import com.chacha.multitenantsaas.repository.OutboundWebhookEventRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class OutboundWebhookEventService {

    private final TenantRepository tenantRepository;
    private final OutboundWebhookEndpointRepository endpointRepository;
    private final OutboundWebhookEventRepository eventRepository;
    private final OutboundWebhookDeliveryRepository deliveryRepository;
    private final JsonMapper jsonMapper;

    public OutboundWebhookEventService(
            TenantRepository tenantRepository,
            OutboundWebhookEndpointRepository endpointRepository,
            OutboundWebhookEventRepository eventRepository,
            OutboundWebhookDeliveryRepository deliveryRepository,
            JsonMapper jsonMapper) {
        this.tenantRepository = tenantRepository;
        this.endpointRepository = endpointRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Persists one immutable event envelope and creates one durable delivery per enabled
     * subscriber. Callers may invoke this inside an existing domain transaction so business state
     * and webhook intent commit or roll back together.
     */
    @Transactional
    public UUID publish(UUID tenantId, OutboundWebhookEventType eventType, Object data) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant not found: " + tenantId));
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        String payloadJson = serializeEnvelope(eventId, tenantId, eventType, occurredAt, data);

        OutboundWebhookEvent event =
                eventRepository.save(
                        new OutboundWebhookEvent(
                                eventId, tenant, eventType, payloadJson, occurredAt));
        List<OutboundWebhookEndpoint> subscribers =
                endpointRepository.findEnabledSubscribers(tenantId, eventType);
        if (!subscribers.isEmpty()) {
            deliveryRepository.saveAll(
                    subscribers.stream()
                            .map(
                                    endpoint ->
                                            new OutboundWebhookDelivery(
                                                    tenant, event, endpoint, occurredAt))
                            .toList());
        }
        return eventId;
    }

    private String serializeEnvelope(
            UUID eventId,
            UUID tenantId,
            OutboundWebhookEventType eventType,
            Instant occurredAt,
            Object data) {
        ObjectNode envelope = jsonMapper.createObjectNode();
        envelope.put("id", eventId.toString());
        envelope.put("type", eventType.wireName());
        envelope.put("tenantId", tenantId.toString());
        envelope.put("occurredAt", occurredAt.toString());
        envelope.set("data", jsonMapper.valueToTree(data));
        try {
            return jsonMapper.writeValueAsString(envelope);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Webhook event payload could not be serialized", exception);
        }
    }
}
