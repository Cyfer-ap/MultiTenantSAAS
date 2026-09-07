package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderVerificationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.exception.IdentityProviderVerificationException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantIdentityProviderVerificationManagementServiceTest {

    @Mock private TenantIdentityProviderVerificationStateService stateService;
    @Mock private OidcProviderVerificationService providerVerificationService;
    @Mock private AppUser actor;

    private TenantIdentityProviderVerificationManagementService service;
    private UUID tenantId;
    private UUID providerId;
    private OidcProviderVerificationInput input;
    private TenantIdentityProviderVerificationSnapshot snapshot;

    @BeforeEach
    void setUp() {
        service =
                new TenantIdentityProviderVerificationManagementService(
                        stateService, providerVerificationService);
        tenantId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        input =
                new OidcProviderVerificationInput(
                        tenantId,
                        IdentityProviderProtocol.OIDC,
                        "https://idp.example.com",
                        "client-id",
                        "encrypted-secret",
                        Set.of("openid", "profile", "email"),
                        "Enterprise IdP");
        snapshot = new TenantIdentityProviderVerificationSnapshot(providerId, 7L, input);
    }

    @Test
    void verifiesSnapshotBeforeCommittingVerifiedState() {
        OidcProviderVerificationResult verification =
                new OidcProviderVerificationResult(
                        "https://idp.example.com",
                        "https://idp.example.com/authorize",
                        "https://idp.example.com/token",
                        "https://idp.example.com/jwks");
        TenantIdentityProviderVerificationResponse expected =
                new TenantIdentityProviderVerificationResponse(
                        providerId,
                        TenantIdentityProviderStatus.VERIFIED,
                        verification.issuer(),
                        verification.authorizationEndpoint(),
                        verification.tokenEndpoint(),
                        verification.jwkSetUri(),
                        Instant.now());

        when(stateService.loadSnapshot(tenantId, actor)).thenReturn(snapshot);
        when(providerVerificationService.verify(input)).thenReturn(verification);
        when(stateService.markVerified(tenantId, providerId, 7L, actor, verification))
                .thenReturn(expected);

        assertThat(service.verify(tenantId, actor)).isSameAs(expected);

        verify(stateService).loadSnapshot(tenantId, actor);
        verify(providerVerificationService).verify(input);
        verify(stateService).markVerified(tenantId, providerId, 7L, actor, verification);
    }

    @Test
    void doesNotCommitStateWhenRemoteVerificationFails() {
        when(stateService.loadSnapshot(tenantId, actor)).thenReturn(snapshot);
        when(providerVerificationService.verify(input))
                .thenThrow(
                        new IdentityProviderVerificationException("Provider verification failed"));

        assertThatThrownBy(() -> service.verify(tenantId, actor))
                .isInstanceOf(IdentityProviderVerificationException.class)
                .hasMessage("Provider verification failed");

        verify(stateService, never())
                .markVerified(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }
}
