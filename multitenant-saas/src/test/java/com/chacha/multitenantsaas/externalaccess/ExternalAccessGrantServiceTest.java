package com.chacha.multitenantsaas.externalaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.projects.access.ProjectAccessPort;
import com.chacha.multitenantsaas.projects.access.ProjectAccessSnapshot;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.SecureTokenService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ExternalAccessGrantServiceTest {

    @Mock private ExternalAccessGrantRepository grantRepository;
    @Mock private ExternalAccessGrantCapabilityRepository capabilityRepository;
    @Mock private ProjectAccessPort projectAccessPort;
    @Mock private CurrentActorService currentActorService;
    @Mock private SecureTokenService secureTokenService;
    @Mock private Jwt jwt;
    @Mock private AppUser actor;

    private ExternalAccessGrantService service;

    @BeforeEach
    void setUp() {
        service =
                new ExternalAccessGrantService(
                        grantRepository,
                        capabilityRepository,
                        projectAccessPort,
                        currentActorService,
                        secureTokenService,
                        new ExternalAccessProperties());
    }

    @Test
    void createReturnsOneTimeSecretButStoresOnlyItsHash() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(projectAccessPort.requireProject(tenantId, projectId))
                .thenReturn(new ProjectAccessSnapshot(projectId, ProjectStatus.ACTIVE));
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(actor.getId()).thenReturn(actorId);
        when(secureTokenService.generateToken()).thenReturn("invite-raw");
        when(secureTokenService.hashToken("invite-raw")).thenReturn("invite-hash");
        when(grantRepository.saveAndFlush(any(ExternalAccessGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExternalAccessDtos.GrantCreatedResponse response =
                service.create(
                        tenantId,
                        projectId,
                        new ExternalAccessDtos.CreateGrantRequest(
                                "Client Reviewer",
                                "CLIENT@example.com",
                                Instant.now().plusSeconds(3600),
                                Set.of(
                                        ExternalAccessCapability.PROJECT_READ,
                                        ExternalAccessCapability.TASK_READ)),
                        jwt);

        assertThat(response.invitationToken()).isEqualTo("invite-raw");
        ArgumentCaptor<ExternalAccessGrant> grantCaptor =
                ArgumentCaptor.forClass(ExternalAccessGrant.class);
        verify(grantRepository).saveAndFlush(grantCaptor.capture());
        assertThat(grantCaptor.getValue().getInvitationTokenHash())
                .isEqualTo("invite-hash")
                .isNotEqualTo("invite-raw");
        assertThat(grantCaptor.getValue().getGuestEmail()).isEqualTo("client@example.com");
        verify(capabilityRepository).saveAll(any());
    }

    @Test
    void archivedProjectCannotCreateExternalGrant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(projectAccessPort.requireProject(tenantId, projectId))
                .thenReturn(new ProjectAccessSnapshot(projectId, ProjectStatus.ARCHIVED));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        tenantId,
                                        projectId,
                                        new ExternalAccessDtos.CreateGrantRequest(
                                                "Client",
                                                "client@example.com",
                                                Instant.now().plusSeconds(3600),
                                                Set.of(ExternalAccessCapability.PROJECT_READ)),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Archived");

        verify(currentActorService, never()).getRequiredActiveActor(any(), any());
    }

    @Test
    void projectReadCapabilityIsMandatory() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(projectAccessPort.requireProject(tenantId, projectId))
                .thenReturn(new ProjectAccessSnapshot(projectId, ProjectStatus.ACTIVE));
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(actor.getId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        tenantId,
                                        projectId,
                                        new ExternalAccessDtos.CreateGrantRequest(
                                                "Client",
                                                "client@example.com",
                                                Instant.now().plusSeconds(3600),
                                                Set.of(ExternalAccessCapability.TASK_READ)),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PROJECT_READ");
    }
}
