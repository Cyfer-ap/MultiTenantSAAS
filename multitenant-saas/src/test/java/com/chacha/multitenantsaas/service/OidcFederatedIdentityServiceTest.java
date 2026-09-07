package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantFederatedIdentity;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantFederatedIdentityRepository;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OidcFederatedIdentityServiceTest {

    @Mock private TenantIdentityProviderRepository identityProviderRepository;
    @Mock private TenantFederatedIdentityRepository federatedIdentityRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private TenantIdentityProvider provider;
    @Mock private Tenant tenant;
    @Mock private AppUser user;

    private OidcFederatedIdentityService service;
    private UUID tenantId;
    private UUID providerId;
    private OidcCallbackTransactionSnapshot transaction;

    @BeforeEach
    void setUp() {
        service =
                new OidcFederatedIdentityService(
                        identityProviderRepository,
                        federatedIdentityRepository,
                        appUserRepository,
                        auditLogService);
        tenantId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        transaction =
                new OidcCallbackTransactionSnapshot(
                        tenantId,
                        providerId,
                        3L,
                        "https://idp.example.com",
                        "client-id",
                        "ciphertext",
                        Set.of("openid", "email"),
                        "Enterprise IdP",
                        "nonce-hash",
                        "pkce-ciphertext",
                        false);
    }

    @Test
    void refusesFirstLinkWhenProviderEmailIsNotVerified() {
        stubCurrentProvider();
        String issuerHash = OidcSecuritySupport.sha256Hex("https://idp.example.com");
        when(federatedIdentityRepository.findByTenant_IdAndIssuerHashAndSubject(
                        tenantId, issuerHash, "subject-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.resolveUser(
                                        transaction,
                                        new OidcVerifiedIdentity(
                                                "https://idp.example.com",
                                                "subject-1",
                                                "user@example.com",
                                                false)))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("OIDC authentication failed");

        verify(appUserRepository, never())
                .findByTenantIdAndEmailForUpdate(any(UUID.class), any(String.class));
        verify(federatedIdentityRepository, never())
                .saveAndFlush(any(TenantFederatedIdentity.class));
    }

    @Test
    void linksVerifiedSubjectOnlyToExistingActiveTenantUser() {
        stubCurrentProvider();
        UUID userId = UUID.randomUUID();
        String issuer = "https://idp.example.com";
        String issuerHash = OidcSecuritySupport.sha256Hex(issuer);
        when(federatedIdentityRepository.findByTenant_IdAndIssuerHashAndSubject(
                        tenantId, issuerHash, "subject-1"))
                .thenReturn(Optional.empty());
        when(appUserRepository.findByTenantIdAndEmailForUpdate(tenantId, "user@example.com"))
                .thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getId()).thenReturn(userId);
        when(federatedIdentityRepository.findByIdentityProvider_IdAndUser_IdAndIssuerHash(
                        providerId, userId, issuerHash))
                .thenReturn(Optional.empty());

        UUID resolved =
                service.resolveUser(
                        transaction,
                        new OidcVerifiedIdentity(issuer, "subject-1", " User@Example.com ", true));

        assertThat(resolved).isEqualTo(userId);
        verify(federatedIdentityRepository).saveAndFlush(any(TenantFederatedIdentity.class));
        verify(auditLogService)
                .recordSuccess(
                        tenant,
                        user,
                        user,
                        AuditAction.IDENTITY_PROVIDER_IDENTITY_LINKED,
                        "Linked verified OIDC identity to tenant user");
    }

    @Test
    void refusesAutomaticProvisioningWhenVerifiedEmailHasNoTenantUser() {
        stubCurrentProvider();
        String issuer = "https://idp.example.com";
        String issuerHash = OidcSecuritySupport.sha256Hex(issuer);
        when(federatedIdentityRepository.findByTenant_IdAndIssuerHashAndSubject(
                        tenantId, issuerHash, "subject-2"))
                .thenReturn(Optional.empty());
        when(appUserRepository.findByTenantIdAndEmailForUpdate(tenantId, "missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.resolveUser(
                                        transaction,
                                        new OidcVerifiedIdentity(
                                                issuer, "subject-2", "missing@example.com", true)))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("OIDC authentication failed");

        verify(federatedIdentityRepository, never())
                .saveAndFlush(any(TenantFederatedIdentity.class));
    }

    private void stubCurrentProvider() {
        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(provider));
        when(provider.getTenant()).thenReturn(tenant);
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(provider.getStatus()).thenReturn(TenantIdentityProviderStatus.VERIFIED);
        when(provider.getId()).thenReturn(providerId);
        when(provider.getVersion()).thenReturn(3L);
        when(provider.getIssuerUri()).thenReturn("https://idp.example.com");
    }
}
