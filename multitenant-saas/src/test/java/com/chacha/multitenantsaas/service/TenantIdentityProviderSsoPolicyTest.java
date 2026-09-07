package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.TenantSsoPolicyUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantSsoMode;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantIdentityProviderSsoPolicyTest {

    @Mock private TenantIdentityProviderRepository identityProviderRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private IdentityProviderIssuerValidator issuerValidator;
    @Mock private IdentityProviderSecretCipher secretCipher;
    @Mock private AuditLogService auditLogService;
    @Mock private Tenant tenant;
    @Mock private AppUser actor;
    @Mock private TenantIdentityProvider identityProvider;

    private TenantIdentityProviderService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service =
                new TenantIdentityProviderService(
                        identityProviderRepository,
                        tenantRepository,
                        appUserRepository,
                        issuerValidator,
                        secretCipher,
                        auditLogService);
        tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(tenant.getId()).thenReturn(tenantId);
        when(actor.getTenant()).thenReturn(tenant);
        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(identityProvider));
    }

    @Test
    void rejectsRequiredSsoUntilProviderIsVerified() {
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.DRAFT);

        assertThatThrownBy(
                        () ->
                                service.updateSsoPolicy(
                                        tenantId,
                                        actor,
                                        new TenantSsoPolicyUpdateRequest(TenantSsoMode.REQUIRED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verified");

        verify(identityProvider, never()).updateSsoMode(any(), any(), any(Instant.class));
    }

    @Test
    void rejectsRequiredSsoWithoutPasswordBreakGlassAdministrator() {
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.VERIFIED);
        when(appUserRepository.countUsersWithPasswordByTenantRoleAndStatus(
                        tenantId, UserRole.TENANT_ADMIN, UserStatus.ACTIVE))
                .thenReturn(0L);

        assertThatThrownBy(
                        () ->
                                service.updateSsoPolicy(
                                        tenantId,
                                        actor,
                                        new TenantSsoPolicyUpdateRequest(TenantSsoMode.REQUIRED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant administrator");

        verify(identityProvider, never()).updateSsoMode(any(), any(), any(Instant.class));
    }

    @Test
    void enablesRequiredSsoWhenProviderAndRecoveryPrerequisitesAreValid() {
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.VERIFIED);
        when(identityProvider.getTenant()).thenReturn(tenant);
        when(identityProvider.getSsoMode()).thenReturn(TenantSsoMode.REQUIRED);
        when(appUserRepository.countUsersWithPasswordByTenantRoleAndStatus(
                        tenantId, UserRole.TENANT_ADMIN, UserStatus.ACTIVE))
                .thenReturn(1L);

        service.updateSsoPolicy(
                tenantId, actor, new TenantSsoPolicyUpdateRequest(TenantSsoMode.REQUIRED));

        verify(identityProvider).updateSsoMode(any(TenantSsoMode.class), any(), any(Instant.class));
        verify(identityProviderRepository).save(identityProvider);
    }

    @Test
    void optionalPolicyDoesNotRequireVerifiedProviderOrBreakGlassCount() {
        when(identityProvider.getTenant()).thenReturn(tenant);
        when(identityProvider.getSsoMode()).thenReturn(TenantSsoMode.OPTIONAL);

        service.updateSsoPolicy(
                tenantId, actor, new TenantSsoPolicyUpdateRequest(TenantSsoMode.OPTIONAL));

        verify(identityProvider).updateSsoMode(any(TenantSsoMode.class), any(), any(Instant.class));
        verify(appUserRepository, never())
                .countUsersWithPasswordByTenantRoleAndStatus(any(), any(), any());
    }
}
