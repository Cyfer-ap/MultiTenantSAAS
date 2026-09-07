package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantSsoMode;
import com.chacha.multitenantsaas.entity.UserRole;
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
class TenantSsoLoginPolicyGuardTest {

    @Mock private TenantIdentityProviderRepository identityProviderRepository;
    @Mock private TenantIdentityProvider identityProvider;
    @Mock private AppUser user;

    private TenantSsoLoginPolicyGuard guard;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        guard = new TenantSsoLoginPolicyGuard(identityProviderRepository);
        tenantId = UUID.randomUUID();
    }

    @Test
    void blocksTenantUserPasswordLoginWhenVerifiedProviderRequiresSso() {
        stubRequiredProvider();
        when(user.getRole()).thenReturn(UserRole.TENANT_USER);

        assertThatThrownBy(() -> guard.enforcePasswordLogin(tenantId, user))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("This workspace requires single sign-on");
    }

    @Test
    void allowsTenantAdministratorOnlyAsBreakGlassWhenSsoIsRequired() {
        stubRequiredProvider();
        when(user.getRole()).thenReturn(UserRole.TENANT_ADMIN);

        assertThat(guard.enforcePasswordLogin(tenantId, user)).isTrue();
    }

    @Test
    void failsOpenToPasswordWhenProviderIsNoLongerVerified() {
        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(identityProvider));
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.DRAFT);

        assertThat(guard.enforcePasswordLogin(tenantId, user)).isFalse();
    }

    @Test
    void allowsNormalPasswordLoginWhenNoProviderExists() {
        when(identityProviderRepository.findByTenant_Id(tenantId)).thenReturn(Optional.empty());

        assertThat(guard.enforcePasswordLogin(tenantId, user)).isFalse();
    }

    private void stubRequiredProvider() {
        when(identityProviderRepository.findByTenant_Id(tenantId))
                .thenReturn(Optional.of(identityProvider));
        when(identityProvider.getStatus()).thenReturn(TenantIdentityProviderStatus.VERIFIED);
        when(identityProvider.getSsoMode()).thenReturn(TenantSsoMode.REQUIRED);
    }
}
