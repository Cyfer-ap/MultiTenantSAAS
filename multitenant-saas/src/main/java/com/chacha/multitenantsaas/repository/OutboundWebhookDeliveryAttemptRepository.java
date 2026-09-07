package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundWebhookDeliveryAttemptRepository
        extends JpaRepository<OutboundWebhookDeliveryAttempt, UUID> {

    Optional<OutboundWebhookDeliveryAttempt> findByDelivery_IdAndLeaseToken(
            UUID deliveryId, UUID leaseToken);

    List<OutboundWebhookDeliveryAttempt> findAllByDelivery_IdOrderByStartedAtAscIdAsc(
            UUID deliveryId);
}
