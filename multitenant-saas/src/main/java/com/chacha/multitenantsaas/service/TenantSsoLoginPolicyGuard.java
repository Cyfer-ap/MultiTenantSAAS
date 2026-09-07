package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantSsoMode;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TenantSsoLoginPolicyGuard {

    private final TenantIdentityProviderRepository identityProviderRepository;

    public TenantSsoLoginPolicyGuard(TenantIdentityProviderRepository identityProviderRepository) {
        this.identityProviderRepository = identityProviderRepository;
    }

    /**
     * Enforces the effective tenant password-login policy.
     *
     * @return {@code true} only when a tenant administrator is using the preserved break-glass path
     */
    public boolean enforcePasswordLogin(UUID tenantId, AppUser user) {
        return identityProviderRepository
                .findByTenant_Id(tenantId)
                .filter(
                        identityProvider ->
                                identityProvider.getStatus()
                                        == TenantIdentityProviderStatus.VERIFIED)
                .filter(identityProvider -> identityProvider.getSsoMode() == TenantSsoMode.REQUIRED)
                .map(
                        identityProvider -> {
                            if (user.getRole() == UserRole.TENANT_ADMIN) {
                                return true;
                            }
                            throw new AuthenticationFailedException(
                                    "This workspace requires single sign-on");
                        })
                .orElse(false);
    }
}
