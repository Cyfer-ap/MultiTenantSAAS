package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderVerificationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.IdentityProviderVerificationException;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantIdentityProviderVerificationStateServiceTest {

    @Mock private TenantIdentityProviderRepository identityProviderRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private TenantIdentityProvider identityProvider;
    @Mock private Tenant tenant;
    @Mock private AppUser actor;

    private TenantIdentityProviderVerificationStateService service;
    private UUID tenantId;
    private UUID providerId;

    @BeforeEach
    void setUp() {
        service =
                new TenantIdentityProviderVerificationStateService(
                        identityProviderRepository, auditLogService);
        tenantId = UUID.randomUUID();
        providerId = UUID.randomUUID();
    }

    @Test
    void loadsImmutableSnapshotFromActiveNonDisabledConfiguration() {
        stubMutableState(TenantIdentityProviderStatus.DRAFT);
        when(identityProvider.getId()).thenReturn(providerId);
        when(identityProvider.getVersion()).thenReturn(4L);
        when(identityProvider.getProtocol()).thenReturn(IdentityProviderProtocol.OIDC);
        when(identityProvider.getIssuerUri()).thenReturn("https://idp.example.com");
        when(identityProvider.getClientId()).thenReturn("client-id");
        when(identityProvider.getClientSecretCiphertext()).thenReturn("ciphertext");
        when(identityProvider.getScopes()).thenReturn(Set.of("openid", "email"));
        when(identityProvider.getDisplayName()).thenReturn("Enterprise IdP");

        TenantIdentityProviderVerificationSnapshot snapshot = service.loadSnapshot(tenantId, actor);

        assertThat(snapshot.identityProviderId()).isEqualTo(providerId);
        assertThat(snapshot.version()).isEqualTo(4L);
        assertThat(snapshot.input().tenantId()).isEqualTo(tenantId);
        assertThat(snapshot.input().issuerUri()).isEqualTo("https://idp.example.com");
        assertThat(snapshot.input().scopes()).containsExactlyInAnyOrder("openid", "email");
    }

    @Test
    void rejectsVerifiedCommitWhenConfigurationChangedDuringRemoteVerification() {
        stubMutableState(TenantIdentityProviderStatus.DRAFT);
        when(identityProvider.getId()).thenReturn(providerId);
        when(identityProvider.getVersion()).thenReturn(5L);
        OidcProviderVerificationResult verification =
                new OidcProviderVerificationResult(
                        "https://idp.example.com",
                        "https://idp.example.com/authorize",
                        "https://idp.example.com/token",
                        "https://idp.example.com/jwks");

        assertThatThrownBy(
                        () -> service.markVerified(tenantId, providerId, 4L, actor, verification))
                .isInstanceOf(IdentityProviderVerificationException.class)
                .hasMessage(
                        "Identity-provider configuration changed during verification; verify it again");

        verify(identityProvider, never()).markVerified(actor, Instant.now());
        verify(identityProviderRepository, never()).save(identityProvider);
    }

    @Test
    void commitsVerifiedStateAndAuditWhenVersionStillMatches() {
        stubMutableState(TenantIdentityProviderStatus.DRAFT);
        when(identityProvider.getId()).thenReturn(providerId);
        when(identityProvider.getVersion()).thenReturn(4L);
        when(identityProvider.getStatus())
                .thenReturn(
                        TenantIdentityProviderStatus.DRAFT, TenantIdentityProviderStatus.VERIFIED);
        Instant verifiedAt = Instant.parse("2026-09-07T17:00:00Z");
        when(identityProvider.getVerifiedAt()).thenReturn(verifiedAt);
        OidcProviderVerificationResult verification =
                new OidcProviderVerificationResult(
                        "https://idp.example.com",
                        "https://idp.example.com/authorize",
                        "https://idp.example.com/token",
                        "https://idp.example.com/jwks");

        TenantIdentityProviderVerificationResponse response =
                service.markVerified(tenantId, providerId, 4L, actor, verification);

        assertThat(response.status()).isEqualTo(TenantIdentityProviderStatus.VERIFIED);
        assertThat(response.verifiedAt()).isEqualTo(verifiedAt);
        verify(identityProviderRepository).save(identityProvider);
        verify(auditLogService)
                .recordSelfSuccess(
                        tenant,
                        actor,
                        AuditAction.IDENTITY_PROVIDER_VERIFIED,
                        "Verified tenant identity provider " + providerId);
    }

    private void stubMutableState(TenantIdentityProviderStatus status) {
        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(identityProvider));
        when(identityProvider.getTenant()).thenReturn(tenant);
        when(actor.getTenant()).thenReturn(tenant);
        when(tenant.getId()).thenReturn(tenantId);
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(identityProvider.getStatus()).thenReturn(status);
    }
}
