package com.chacha.multitenantsaas.externalaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalPortalServiceTest {

    @Mock private ExternalGuestSessionService sessionService;
    @Mock private ExternalProjectProjectionPort projectProjectionPort;
    @Mock private ExternalTaskProjectionPort taskProjectionPort;

    private ExternalPortalService service;

    @BeforeEach
    void setUp() {
        service =
                new ExternalPortalService(
                        sessionService,
                        projectProjectionPort,
                        taskProjectionPort,
                        new ExternalAccessProperties());
    }

    @Test
    void taskReadUsesOnlyGrantBoundTenantAndProject() {
        UUID grantId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(3600);
        ExternalGuestSessionContext context =
                new ExternalGuestSessionContext(
                        grantId,
                        tenantId,
                        projectId,
                        "Client",
                        "client@example.com",
                        Set.of(
                                ExternalAccessCapability.PROJECT_READ,
                                ExternalAccessCapability.TASK_READ),
                        expiry,
                        expiry);
        when(sessionService.requireSession("session")).thenReturn(context);
        when(taskProjectionPort.listTasks(tenantId, projectId, 100))
                .thenReturn(
                        List.of(
                                new ExternalTaskSnapshot(
                                        UUID.randomUUID(),
                                        "Shared task",
                                        "Visible description",
                                        ProjectTaskStatus.TODO,
                                        ProjectTaskPriority.HIGH,
                                        null,
                                        null,
                                        Instant.now())));

        ExternalAccessDtos.TasksResponse response = service.tasks("session");

        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().getFirst().title()).isEqualTo("Shared task");
        verify(taskProjectionPort).listTasks(tenantId, projectId, 100);
    }

    @Test
    void missingTaskCapabilityStopsBeforeTaskDomainRead() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(3600);
        when(sessionService.requireSession("session"))
                .thenReturn(
                        new ExternalGuestSessionContext(
                                UUID.randomUUID(),
                                tenantId,
                                projectId,
                                "Client",
                                "client@example.com",
                                Set.of(ExternalAccessCapability.PROJECT_READ),
                                expiry,
                                expiry));

        assertThatThrownBy(() -> service.tasks("session"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not allow");

        verify(taskProjectionPort, never()).listTasks(tenantId, projectId, 100);
    }
}
