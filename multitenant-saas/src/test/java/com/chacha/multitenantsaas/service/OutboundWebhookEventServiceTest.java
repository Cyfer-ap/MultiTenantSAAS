package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.OutboundWebhookDeliveryRepository;
import com.chacha.multitenantsaas.repository.OutboundWebhookEndpointRepository;
import com.chacha.multitenantsaas.repository.OutboundWebhookEventRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OutboundWebhookEventServiceTest {

    @Test
    void skipsOutboxPersistenceWhenNoEndpointSubscribes() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        OutboundWebhookEndpointRepository endpointRepository =
                mock(OutboundWebhookEndpointRepository.class);
        OutboundWebhookEventRepository eventRepository = mock(OutboundWebhookEventRepository.class);
        OutboundWebhookDeliveryRepository deliveryRepository =
                mock(OutboundWebhookDeliveryRepository.class);
        JsonMapper jsonMapper = mock(JsonMapper.class);
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = mock(Tenant.class);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(endpointRepository.findEnabledSubscribers(
                        tenantId, OutboundWebhookEventType.PROJECT_CREATED))
                .thenReturn(List.of());

        OutboundWebhookEventService service =
                new OutboundWebhookEventService(
                        tenantRepository,
                        endpointRepository,
                        eventRepository,
                        deliveryRepository,
                        jsonMapper);

        UUID eventId =
                service.publish(
                        tenantId,
                        OutboundWebhookEventType.PROJECT_CREATED,
                        java.util.Map.of("projectId", UUID.randomUUID()));

        assertThat(eventId).isNotNull();
        verifyNoInteractions(eventRepository, deliveryRepository, jsonMapper);
    }
}
