package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OutboundWebhookEndpoint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundWebhookEndpointRepository
        extends JpaRepository<OutboundWebhookEndpoint, UUID> {

    List<OutboundWebhookEndpoint> findAllByTenant_IdAndArchivedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId);

    Optional<OutboundWebhookEndpoint> findByIdAndTenant_IdAndArchivedAtIsNull(
            UUID id, UUID tenantId);

    Optional<OutboundWebhookEndpoint> findByIdAndTenant_Id(UUID id, UUID tenantId);
}
