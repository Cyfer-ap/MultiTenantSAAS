package com.chacha.multitenantsaas.projects.creation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.AuditLogService;
import com.chacha.multitenantsaas.service.OutboundWebhookEventService;
import com.chacha.multitenantsaas.service.ProjectMemberService;
import com.chacha.multitenantsaas.service.SubscriptionQuotaGuardService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProjectCreationAdapterTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private ProjectMemberService projectMemberService;
    @Mock private AuditLogService auditLogService;
    @Mock private SubscriptionQuotaGuardService subscriptionQuotaGuardService;
    @Mock private OutboundWebhookEventService outboundWebhookEventService;

    @Test
    void createsProjectThroughOrdinaryLifecycleInvariants() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-15T00:00:00Z");
        Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
        AppUser actor = org.mockito.Mockito.mock(AppUser.class);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(appUserRepository.findByTenantIdAndId(tenantId, actorId))
                .thenReturn(Optional.of(actor));
        when(actor.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(actor.getId()).thenReturn(actorId);
        when(actor.getFullName()).thenReturn("Template Owner");
        when(actor.getEmail()).thenReturn("owner@example.test");
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(
                        invocation -> {
                            Project project = invocation.getArgument(0);
                            project.setId(projectId);
                            project.setCreatedAt(now);
                            project.setUpdatedAt(now);
                            return project;
                        });

        DefaultProjectCreationAdapter adapter = adapter();
        ProjectCreationResult result =
                adapter.createProject(
                        new ProjectCreationCommand(
                                tenantId,
                                actorId,
                                "  Release workspace  ",
                                "  From a project template  ",
                                ProjectStatus.ACTIVE));

        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(result.name()).isEqualTo("Release workspace");
        assertThat(result.description()).isEqualTo("From a project template");
        assertThat(result.createdByUserId()).isEqualTo(actorId);
        verify(subscriptionQuotaGuardService).requireProjectSlot(tenantId);
        verify(projectMemberService).addCreatorAsProjectLead(any(Project.class), eq(actor));
        verify(auditLogService).recordSuccess(eq(tenant), eq(actor), eq(actor), any(), any());
        verify(outboundWebhookEventService).publish(eq(tenantId), any(), any());
    }

    @Test
    void quotaFailureStopsBeforeActorValidationAndPersistence() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        doThrow(new IllegalStateException("Project quota exceeded"))
                .when(subscriptionQuotaGuardService)
                .requireProjectSlot(tenantId);

        assertThatThrownBy(
                        () ->
                                adapter()
                                        .createProject(
                                                new ProjectCreationCommand(
                                                        tenantId,
                                                        actorId,
                                                        "Project",
                                                        null,
                                                        ProjectStatus.PLANNING)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quota");

        verify(appUserRepository, never()).findByTenantIdAndId(any(), any());
        verify(projectRepository, never()).save(any());
        verify(projectMemberService, never()).addCreatorAsProjectLead(any(), any());
    }

    private DefaultProjectCreationAdapter adapter() {
        return new DefaultProjectCreationAdapter(
                projectRepository,
                tenantRepository,
                appUserRepository,
                projectMemberService,
                auditLogService,
                subscriptionQuotaGuardService,
                outboundWebhookEventService);
    }
}
