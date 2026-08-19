package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.NotificationDelivery;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationDeliveryStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    Optional<NotificationDelivery> findByNotification_IdAndChannel(
            UUID notificationId, NotificationDeliveryChannel channel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select delivery from NotificationDelivery delivery
            join fetch delivery.tenant
            join fetch delivery.notification notification
            join fetch notification.recipientUser
            where (delivery.status in :readyStatuses and delivery.nextAttemptAt <= :now)
               or (delivery.status = :processingStatus
                   and delivery.processingStartedAt <= :staleBefore)
            order by delivery.createdAt, delivery.id
            """)
    List<NotificationDelivery> findClaimableForUpdate(
            @Param("readyStatuses") Collection<NotificationDeliveryStatus> readyStatuses,
            @Param("processingStatus") NotificationDeliveryStatus processingStatus,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select delivery from NotificationDelivery delivery where delivery.id = :deliveryId")
    Optional<NotificationDelivery> findByIdForUpdate(@Param("deliveryId") UUID deliveryId);
}
