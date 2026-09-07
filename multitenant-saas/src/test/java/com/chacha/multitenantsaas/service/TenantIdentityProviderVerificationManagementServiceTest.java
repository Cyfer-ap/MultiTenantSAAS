package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderVerificationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantIdentityProviderVerificationManagementServiceTest {

    @Mock private TenantIdentityProviderRepository identityProviderRepository;
    @Mock private OidcProviderVerificationService providerVerificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private TenantIdentityProvider identityProvider;
    @Mock private Tenant tenant;
    @Mock private AppUser actor;

    private TenantIdentityProviderVerificationManagementService service;
    private UUID tenantId;
    private UUID providerId;

    @BeforeEach
    void setUp() {
        service =
                new TenantIdentityProviderVerificationManagementService(
                        identityProviderRepository, providerVerificationService, auditLogService);
        tenantId = UUID.randomUUID();
        providerId = UUID.randomUUID();

        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(identityProvider));
        when(identityProvider.getTenant()).thenReturn(tenant);
        when(tenant.getId()).thenReturn(tenantId);
        when(actor.getTenant()).thenReturn(tenant);
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(identityProvider.getId()).thenReturn(providerId);
    }

    @Test
    void marksProviderVerifiedOnlyAfterRuntimeVerificationSucceeds() {
        OidcProviderVerificationResult verification =
                new OidcProviderVerificationResult(
                        "https://idp.example.com",
                        "https://idp.example.com/authorize",
                        "https://idp.example.com/token",
                        "https://idp.example.com/jwks");
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.VERIFIED);
        when(providerVerificationService.verify(identityProvider)).thenReturn(verification);

        TenantIdentityProviderVerificationResponse response = service.verify(tenantId, actor);

        assertThat(response.id()).isEqualTo(providerId);
        assertThat(response.status()).isEqualTo(TenantIdentityProviderStatus.VERIFIED);
        assertThat(response.issuerUri()).isEqualTo("https://idp.example.com");
        verify(providerVerificationService).verify(identityProvider);
        verify(identityProvider)
                .markVerified(
                        org.mockito.ArgumentMatchers.eq(actor), org.mockito.ArgumentMatchers.any());
        verify(identityProviderRepository).save(identityProvider);
        verify(auditLogService)
                .recordSelfSuccess(
                        tenant,
                        actor,
                        AuditAction.IDENTITY_PROVIDER_VERIFIED,
                        "Verified tenant identity provider " + providerId);
    }

    @Test
    void rejectsDisabledProviderWithoutCallingRemoteRuntime() {
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.DISABLED);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.verify(tenantId, actor))
                .withMessage("Disabled identity-provider configuration cannot be verified");

        verify(providerVerificationService, never()).verify(identityProvider);
        verify(identityProviderRepository, never()).save(identityProvider);
    }

    @Test
    void rejectsActorFromAnotherTenant() {
        Tenant otherTenant = org.mockito.Mockito.mock(Tenant.class);
        when(otherTenant.getId()).thenReturn(UUID.randomUUID());
        when(actor.getTenant()).thenReturn(otherTenant);

        assertThatThrownBy(() -> service.verify(tenantId, actor))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authenticated user does not belong to this tenant");

        verify(providerVerificationService, never()).verify(identityProvider);
    }
}
