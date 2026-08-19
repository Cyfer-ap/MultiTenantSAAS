package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.NotificationPreference;
import com.chacha.multitenantsaas.entity.NotificationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByTenant_IdAndRecipientUser_Id(
            UUID tenantId, UUID recipientUserId);

    Optional<NotificationPreference> findByTenant_IdAndRecipientUser_IdAndType(
            UUID tenantId, UUID recipientUserId, NotificationType type);
}
