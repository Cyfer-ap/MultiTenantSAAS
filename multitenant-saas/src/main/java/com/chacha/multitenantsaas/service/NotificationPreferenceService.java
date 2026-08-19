package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.NotificationPreferenceResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationPreference;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.NotificationPreferenceRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(UUID tenantId, UUID recipientUserId) {
        Map<NotificationType, NotificationPreference> stored = new EnumMap<>(NotificationType.class);
        repository.findByTenant_IdAndRecipientUser_Id(tenantId, recipientUserId)
                .forEach(preference -> stored.put(preference.getType(), preference));

        return Arrays.stream(NotificationType.values())
                .map(type -> toResponse(type, stored.get(type)))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isEmailEnabled(UUID tenantId, UUID recipientUserId, NotificationType type) {
        Objects.requireNonNull(type, "Notification type is required");
        if (!isEmailConfigurable(type)) {
            return true;
        }
        return repository
                .findByTenant_IdAndRecipientUser_IdAndType(tenantId, recipientUserId, type)
                .map(NotificationPreference::isEmailEnabled)
                .orElse(true);
    }

    @Transactional
    public NotificationPreferenceResponse updateEmailPreference(
            Tenant tenant, AppUser recipientUser, NotificationType type, boolean emailEnabled) {
        validateRecipientScope(tenant, recipientUser);
        Objects.requireNonNull(type, "Notification type is required");

        if (!isEmailConfigurable(type) && !emailEnabled) {
            throw new IllegalArgumentException("Security alert email notifications cannot be disabled");
        }

        NotificationPreference preference =
                repository
                        .findByTenant_IdAndRecipientUser_IdAndType(
                                tenant.getId(), recipientUser.getId(), type)
                        .orElseGet(
                                () ->
                                        new NotificationPreference(
                                                tenant, recipientUser, type, emailEnabled));
        preference.setEmailEnabled(!isEmailConfigurable(type) || emailEnabled);
        return toResponse(type, repository.save(preference));
    }

    private NotificationPreferenceResponse toResponse(
            NotificationType type, NotificationPreference preference) {
        boolean configurable = isEmailConfigurable(type);
        boolean emailEnabled = !configurable || preference == null || preference.isEmailEnabled();
        return new NotificationPreferenceResponse(type, true, emailEnabled, configurable);
    }

    private boolean isEmailConfigurable(NotificationType type) {
        return type != NotificationType.SECURITY_ALERT;
    }

    private void validateRecipientScope(Tenant tenant, AppUser recipientUser) {
        Objects.requireNonNull(tenant, "Notification preference tenant is required");
        Objects.requireNonNull(recipientUser, "Notification preference recipient is required");
        if (tenant.getId() == null
                || recipientUser.getTenant() == null
                || !tenant.getId().equals(recipientUser.getTenant().getId())) {
            throw new IllegalArgumentException(
                    "Notification preference recipient must belong to the notification tenant");
        }
    }
}
