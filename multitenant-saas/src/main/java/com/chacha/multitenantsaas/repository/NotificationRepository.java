package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.Notification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByTenant_IdAndRecipientUser_IdOrderByCreatedAtDesc(
            UUID tenantId, UUID recipientUserId, Pageable pageable);

    Optional<Notification> findByTenant_IdAndRecipientUser_IdAndId(
            UUID tenantId, UUID recipientUserId, UUID notificationId);

    long countByTenant_IdAndRecipientUser_IdAndReadAtIsNull(UUID tenantId, UUID recipientUserId);
}
