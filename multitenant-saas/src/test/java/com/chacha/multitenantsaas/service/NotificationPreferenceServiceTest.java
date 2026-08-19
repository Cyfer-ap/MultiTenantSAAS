package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.NotificationPreferenceResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationPreference;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.NotificationPreferenceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock private NotificationPreferenceRepository repository;
    @Mock private Tenant tenant;
    @Mock private AppUser recipient;

    private NotificationPreferenceService service;
    private UUID tenantId;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(repository);
        tenantId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
    }

    @Test
    void defaultsEveryEmailPreferenceToEnabledAndKeepsSecurityMandatory() {
        when(repository.findByTenant_IdAndRecipientUser_Id(tenantId, recipientId))
                .thenReturn(List.of());

        List<NotificationPreferenceResponse> preferences =
                service.getPreferences(tenantId, recipientId);

        assertThat(preferences).hasSize(NotificationType.values().length);
        assertThat(preferences).allMatch(NotificationPreferenceResponse::inAppEnabled);
        assertThat(preferences).allMatch(NotificationPreferenceResponse::emailEnabled);
        assertThat(preferences)
                .filteredOn(preference -> preference.type() == NotificationType.SECURITY_ALERT)
                .singleElement()
                .satisfies(preference -> assertThat(preference.emailConfigurable()).isFalse());
    }

    @Test
    void storedOptOutDisablesOptionalEmailDelivery() {
        NotificationPreference preference =
                new NotificationPreference(
                        tenant, recipient, NotificationType.TASK_COMMENT_ADDED, false);
        when(repository.findByTenant_IdAndRecipientUser_IdAndType(
                        tenantId, recipientId, NotificationType.TASK_COMMENT_ADDED))
                .thenReturn(Optional.of(preference));

        assertThat(
                        service.isEmailEnabled(
                                tenantId, recipientId, NotificationType.TASK_COMMENT_ADDED))
                .isFalse();
    }

    @Test
    void securityAlertEmailCannotBeDisabled() {
        when(tenant.getId()).thenReturn(tenantId);
        when(recipient.getTenant()).thenReturn(tenant);

        assertThatThrownBy(
                        () ->
                                service.updateEmailPreference(
                                        tenant, recipient, NotificationType.SECURITY_ALERT, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be disabled");
    }
}
