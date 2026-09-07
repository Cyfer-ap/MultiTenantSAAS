package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.WorkspaceAuthenticationMode;
import com.chacha.multitenantsaas.dto.WorkspaceLoginOptionResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantSsoMode;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceLoginOptionFactory {

    private final TenantIdentityProviderRepository identityProviderRepository;

    @Autowired
    public WorkspaceLoginOptionFactory(TenantIdentityProviderRepository identityProviderRepository) {
        this.identityProviderRepository = identityProviderRepository;
    }

    WorkspaceLoginOptionFactory() {
        this.identityProviderRepository = null;
    }

    public WorkspaceLoginOptionResponse create(AppUser user) {
        boolean hasPassword = hasUsablePassword(user);
        TenantIdentityProvider identityProvider = findVerifiedProvider(user);

        if (identityProvider == null) {
            return hasPassword ? passwordOnly(user) : null;
        }

        WorkspaceAuthenticationMode mode;
        if (identityProvider.getSsoMode() == TenantSsoMode.REQUIRED) {
            mode = WorkspaceAuthenticationMode.SSO_REQUIRED;
        } else if (hasPassword) {
            mode = WorkspaceAuthenticationMode.PASSWORD_OR_SSO;
        } else {
            mode = WorkspaceAuthenticationMode.SSO_ONLY;
        }

        return new WorkspaceLoginOptionResponse(
                user.getTenant().getId(),
                user.getTenant().getName(),
                user.getTenant().getSlug(),
                mode,
                identityProvider.getDisplayName());
    }

    private TenantIdentityProvider findVerifiedProvider(AppUser user) {
        if (identityProviderRepository == null) {
            return null;
        }

        return identityProviderRepository
                .findByTenant_Id(user.getTenant().getId())
                .filter(
                        identityProvider ->
                                identityProvider.getStatus()
                                        == TenantIdentityProviderStatus.VERIFIED)
                .orElse(null);
    }

    private WorkspaceLoginOptionResponse passwordOnly(AppUser user) {
        return new WorkspaceLoginOptionResponse(
                user.getTenant().getId(), user.getTenant().getName(), user.getTenant().getSlug());
    }

    private boolean hasUsablePassword(AppUser user) {
        return user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
    }
}
