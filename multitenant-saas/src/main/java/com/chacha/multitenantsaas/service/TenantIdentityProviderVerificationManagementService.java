package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderVerificationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TenantIdentityProviderVerificationManagementService {

    private final TenantIdentityProviderVerificationStateService stateService;
    private final OidcProviderVerificationService providerVerificationService;

    public TenantIdentityProviderVerificationManagementService(
            TenantIdentityProviderVerificationStateService stateService,
            OidcProviderVerificationService providerVerificationService) {
        this.stateService = stateService;
        this.providerVerificationService = providerVerificationService;
    }

    public TenantIdentityProviderVerificationResponse verify(UUID tenantId, AppUser actor) {
        TenantIdentityProviderVerificationSnapshot snapshot =
                stateService.loadSnapshot(tenantId, actor);
        OidcProviderVerificationResult verification =
                providerVerificationService.verify(snapshot.input());
        return stateService.markVerified(
                tenantId,
                snapshot.identityProviderId(),
                snapshot.version(),
                actor,
                verification);
    }
}
