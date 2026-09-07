package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.config.OidcLoginProperties;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.entity.OidcSessionHandoff;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.OidcSessionHandoffRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OidcSessionHandoffServiceTest {

    @Mock private OidcSessionHandoffRepository handoffRepository;
    @Mock private SecureTokenService secureTokenService;
    @Mock private OidcSessionService sessionService;

    private OidcSessionHandoffService service;

    @BeforeEach
    void setUp() {
        OidcLoginProperties properties = new OidcLoginProperties();
        properties.setSessionHandoffMinutes(2L);
        service =
                new OidcSessionHandoffService(
                        handoffRepository, secureTokenService, properties, sessionService);
    }

    @Test
    void issueStoresOnlyHashedCodeAndPreservesTenantUserAndSessionBinding() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(secureTokenService.generateToken()).thenReturn("raw-handoff-code");
        when(secureTokenService.hashToken("raw-handoff-code"))
                .thenReturn("0f6e2f9db3cf3aa764f21e58417a497607613932824962f5dce5f6e46d7af46f");

        OidcSessionHandoffService.IssuedHandoff issued = service.issue(tenantId, userId, true);

        ArgumentCaptor<OidcSessionHandoff> captor = ArgumentCaptor.forClass(OidcSessionHandoff.class);
        verify(handoffRepository).save(captor.capture());
        OidcSessionHandoff stored = captor.getValue();

        assertThat(issued.code()).isEqualTo("raw-handoff-code");
        assertThat(stored.getCodeHash()).isNotEqualTo(issued.code());
        assertThat(stored.getCodeHash())
                .isEqualTo("0f6e2f9db3cf3aa764f21e58417a497607613932824962f5dce5f6e46d7af46f");
        assertThat(stored.getTenantId()).isEqualTo(tenantId);
        assertThat(stored.getUserId()).isEqualTo(userId);
        assertThat(stored.isPersistentSession()).isTrue();
        assertThat(stored.getConsumedAt()).isNull();
        assertThat(issued.expiresAt()).isAfter(stored.getCreatedAt());
    }

    @Test
    void handoffCanBeConsumedOnlyOnceAndPreservesPersistentSession() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OidcSessionHandoff handoff =
                new OidcSessionHandoff(
                        "handoff-hash",
                        tenantId,
                        userId,
                        true,
                        Instant.now().minusSeconds(5),
                        Instant.now().plusSeconds(60));
        LoginResponse loginResponse = loginResponse(tenantId, userId, true);

        when(secureTokenService.hashToken("opaque-code")).thenReturn("handoff-hash");
        when(handoffRepository.findByCodeHashForUpdate("handoff-hash"))
                .thenReturn(Optional.of(handoff));
        when(sessionService.issue(tenantId, userId, true)).thenReturn(loginResponse);

        assertThat(service.exchange("opaque-code")).isEqualTo(loginResponse);
        assertThat(handoff.getConsumedAt()).isNotNull();

        assertThatThrownBy(() -> service.exchange("opaque-code"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("OIDC session handoff is invalid or expired");

        verify(sessionService, times(1)).issue(tenantId, userId, true);
        verify(handoffRepository, times(1)).save(handoff);
    }

    @Test
    void expiredHandoffFailsWithoutIssuingSession() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OidcSessionHandoff expired =
                new OidcSessionHandoff(
                        "expired-hash",
                        tenantId,
                        userId,
                        false,
                        Instant.now().minusSeconds(120),
                        Instant.now().minusSeconds(1));

        when(secureTokenService.hashToken("expired-code")).thenReturn("expired-hash");
        when(handoffRepository.findByCodeHashForUpdate("expired-hash"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.exchange("expired-code"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("OIDC session handoff is invalid or expired");

        verify(sessionService, never()).issue(any(UUID.class), any(UUID.class), anyBoolean());
        verify(handoffRepository, never()).save(expired);
    }

    @Test
    void invalidCodeFailsWithoutIssuingSession() {
        when(secureTokenService.hashToken("unknown-code")).thenReturn("unknown-hash");
        when(handoffRepository.findByCodeHashForUpdate("unknown-hash"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchange("unknown-code"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("OIDC session handoff is invalid or expired");

        verify(sessionService, never()).issue(any(UUID.class), any(UUID.class), anyBoolean());
    }

    private LoginResponse loginResponse(UUID tenantId, UUID userId, boolean persistentSession) {
        return new LoginResponse(
                tenantId,
                userId,
                "Grace Hopper",
                "grace@example.com",
                UserRole.TENANT_USER,
                "access-token",
                "refresh-token",
                "csrf-token",
                "Bearer",
                3600L,
                persistentSession,
                "Login successful");
    }
}
