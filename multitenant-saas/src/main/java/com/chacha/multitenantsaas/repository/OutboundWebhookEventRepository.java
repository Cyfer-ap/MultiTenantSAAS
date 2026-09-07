package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OutboundWebhookEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundWebhookEventRepository extends JpaRepository<OutboundWebhookEvent, UUID> {}
