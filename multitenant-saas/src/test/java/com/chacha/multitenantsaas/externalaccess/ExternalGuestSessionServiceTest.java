package com.chacha.multitenantsaas.externalaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.service.SecureTokenService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalGuestSessionServiceTest {

    @Mock private ExternalAccessGrantRepository grantRepository;
    @Mock private ExternalAccessGrantCapabilityRepository capabilityRepository;
    @Mock private ExternalGuestSessionRepository sessionRepository;
    @Mock private SecureTokenService secureTokenService;

    private ExternalGuestSessionService service;

    @BeforeEach
    void setUp() {
        service =
                new ExternalGuestSessionService(
                        grantRepository,
                        capabilityRepository,
                        sessionRepository,
                        secureTokenService);
    }

    @Test
    void invitationExchangePersistsOnlyHashedSessionAndCannotReplay() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ExternalAccessGrant grant =
                new ExternalAccessGrant(
                        tenantId,
                        projectId,
                        UUID.randomUUID(),
                        "Client Reviewer",
                        "client@example.com",
                        "invite-hash",
                        Instant.now().plusSeconds(3600));

        when(secureTokenService.hashToken("invite-raw")).thenReturn("invite-hash");
        when(secureTokenService.generateToken()).thenReturn("session-raw");
        when(secureTokenService.hashToken("session-raw")).thenReturn("session-hash");
        when(grantRepository.findByInvitationTokenHashForUpdate("invite-hash"))
                .thenReturn(Optional.of(grant));

        ExternalAccessDtos.ExchangeResponse response = service.exchange("invite-raw");

        assertThat(response.sessionToken()).isEqualTo("session-raw");
        assertThat(grant.getAcceptedAt()).isNotNull();

        ArgumentCaptor<ExternalGuestSession> sessionCaptor =
                ArgumentCaptor.forClass(ExternalGuestSession.class);
        verify(sessionRepository).saveAndFlush(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getTokenHash())
                .isEqualTo("session-hash")
                .isNotEqualTo("session-raw");

        assertThatThrownBy(() -> service.exchange("invite-raw"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Invalid or unavailable");
    }

    @Test
    void revokedGrantInvalidatesPreviouslyIssuedSession() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ExternalAccessGrant grant =
                new ExternalAccessGrant(
                        tenantId,
                        projectId,
                        UUID.randomUUID(),
                        "Client Reviewer",
                        "client@example.com",
                        "invite-hash",
                        Instant.now().plusSeconds(3600));
        grant.accept(Instant.now());
        grant.revoke(UUID.randomUUID(), Instant.now());

        ExternalGuestSession session =
                new ExternalGuestSession(
                        tenantId,
                        projectId,
                        grant.getId(),
                        "session-hash",
                        grant.getExpiresAt());

        when(secureTokenService.hashToken("session-raw")).thenReturn("session-hash");
        when(sessionRepository.findByTokenHash("session-hash")).thenReturn(Optional.of(session));
        when(grantRepository.findByTenantIdAndProjectIdAndId(
                        tenantId, projectId, grant.getId()))
                .thenReturn(Optional.of(grant));

        assertThatThrownBy(() -> service.requireSession("session-raw"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void validSessionReturnsOnlyPersistedGrantCapabilities() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ExternalAccessGrant grant =
                new ExternalAccessGrant(
                        tenantId,
                        projectId,
                        UUID.randomUUID(),
                        "Client Reviewer",
                        "client@example.com",
                        "invite-hash",
                        Instant.now().plusSeconds(3600));
        grant.accept(Instant.now());
        ExternalGuestSession session =
                new ExternalGuestSession(
                        tenantId,
                        projectId,
                        grant.getId(),
                        "session-hash",
                        grant.getExpiresAt());

        when(secureTokenService.hashToken("session-raw")).thenReturn("session-hash");
        when(sessionRepository.findByTokenHash("session-hash")).thenReturn(Optional.of(session));
        when(grantRepository.findByTenantIdAndProjectIdAndId(
                        tenantId, projectId, grant.getId()))
                .thenReturn(Optional.of(grant));
        when(capabilityRepository.findByTenantIdAndProjectIdAndGrantIdOrderByCapabilityAsc(
                        tenantId, projectId, grant.getId()))
                .thenReturn(
                        List.of(
                                new ExternalAccessGrantCapability(
                                        tenantId,
                                        projectId,
                                        grant.getId(),
                                        ExternalAccessCapability.PROJECT_READ)));

        ExternalGuestSessionContext context = service.requireSession("session-raw");

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.projectId()).isEqualTo(projectId);
        assertThat(context.capabilities())
                .containsExactly(ExternalAccessCapability.PROJECT_READ);
    }
}
