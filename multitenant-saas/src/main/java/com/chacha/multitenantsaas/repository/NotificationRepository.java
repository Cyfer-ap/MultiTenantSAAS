package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.Notification;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification from Notification notification where notification.id = :id")
    Optional<Notification> findByIdForUpdate(@Param("id") UUID id);

    Page<Notification> findByTenant_IdAndRecipientUser_IdOrderByCreatedAtDesc(
            UUID tenantId, UUID recipientUserId, Pageable pageable);

    Optional<Notification> findByTenant_IdAndRecipientUser_IdAndId(
            UUID tenantId, UUID recipientUserId, UUID notificationId);

    long countByTenant_IdAndRecipientUser_IdAndReadAtIsNull(UUID tenantId, UUID recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update Notification notification
            set notification.readAt = :readAt
            where notification.tenant.id = :tenantId
              and notification.recipientUser.id = :recipientUserId
              and notification.readAt is null
            """)
    int markAllRead(
            @Param("tenantId") UUID tenantId,
            @Param("recipientUserId") UUID recipientUserId,
            @Param("readAt") Instant readAt);
}
