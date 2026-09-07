package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.WorkspaceAuthenticationMode;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantSsoMode;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceLoginOptionFactoryTest {

    @Mock private TenantIdentityProviderRepository identityProviderRepository;
    @Mock private AppUser user;
    @Mock private Tenant tenant;
    @Mock private TenantIdentityProvider identityProvider;

    private WorkspaceLoginOptionFactory factory;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        factory = new WorkspaceLoginOptionFactory(identityProviderRepository);
        tenantId = UUID.randomUUID();
        when(user.getTenant()).thenReturn(tenant);
        when(tenant.getId()).thenReturn(tenantId);
        when(tenant.getName()).thenReturn("Acme");
        when(tenant.getSlug()).thenReturn("acme");
    }

    @Test
    void exposesPasswordOnlyWhenNoVerifiedProviderExists() {
        when(user.getPasswordHash()).thenReturn("hash");
        when(identityProviderRepository.findByTenant_Id(tenantId)).thenReturn(Optional.empty());

        var option = factory.create(user);

        assertThat(option.authenticationMode()).isEqualTo(WorkspaceAuthenticationMode.PASSWORD_ONLY);
        assertThat(option.identityProviderDisplayName()).isNull();
    }

    @Test
    void exposesPasswordOrSsoForOptionalVerifiedProvider() {
        stubVerifiedProvider(TenantSsoMode.OPTIONAL);
        when(user.getPasswordHash()).thenReturn("hash");

        var option = factory.create(user);

        assertThat(option.authenticationMode())
                .isEqualTo(WorkspaceAuthenticationMode.PASSWORD_OR_SSO);
        assertThat(option.identityProviderDisplayName()).isEqualTo("Corporate SSO");
    }

    @Test
    void exposesSsoOnlyForPasswordlessUserWithOptionalVerifiedProvider() {
        stubVerifiedProvider(TenantSsoMode.OPTIONAL);
        when(user.getPasswordHash()).thenReturn(null);

        var option = factory.create(user);

        assertThat(option.authenticationMode()).isEqualTo(WorkspaceAuthenticationMode.SSO_ONLY);
    }

    @Test
    void exposesSsoRequiredWhenVerifiedProviderEnforcesSso() {
        stubVerifiedProvider(TenantSsoMode.REQUIRED);
        when(user.getPasswordHash()).thenReturn("hash");

        var option = factory.create(user);

        assertThat(option.authenticationMode()).isEqualTo(WorkspaceAuthenticationMode.SSO_REQUIRED);
    }

    @Test
    void hidesPasswordlessWorkspaceWhenProviderIsNotVerified() {
        when(user.getPasswordHash()).thenReturn(null);
        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(identityProvider));
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.DRAFT);

        assertThat(factory.create(user)).isNull();
    }

    private void stubVerifiedProvider(TenantSsoMode ssoMode) {
        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(identityProvider));
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.VERIFIED);
        when(identityProvider.getSsoMode()).thenReturn(ssoMode);
        when(identityProvider.getDisplayName()).thenReturn("Corporate SSO");
    }
}
