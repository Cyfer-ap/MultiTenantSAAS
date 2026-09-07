package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderVerificationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantIdentityProviderVerificationManagementService {

    private final TenantIdentityProviderRepository identityProviderRepository;
    private final OidcProviderVerificationService providerVerificationService;
    private final AuditLogService auditLogService;

    public TenantIdentityProviderVerificationManagementService(
            TenantIdentityProviderRepository identityProviderRepository,
            OidcProviderVerificationService providerVerificationService,
            AuditLogService auditLogService) {
        this.identityProviderRepository = identityProviderRepository;
        this.providerVerificationService = providerVerificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TenantIdentityProviderVerificationResponse verify(UUID tenantId, AppUser actor) {
        TenantIdentityProvider identityProvider =
                identityProviderRepository
                        .findByTenant_Id(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant identity-provider configuration not found"));
        Tenant tenant = identityProvider.getTenant();
        requireTenantActor(tenantId, actor);

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Identity federation can only be verified for active tenants");
        }
        if (identityProvider.getStatus() == TenantIdentityProviderStatus.DISABLED) {
            throw new IllegalArgumentException(
                    "Disabled identity-provider configuration cannot be verified");
        }

        OidcProviderVerificationResult verification =
                providerVerificationService.verify(identityProvider);
        Instant now = Instant.now();
        identityProvider.markVerified(actor, now);
        identityProviderRepository.save(identityProvider);

        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.IDENTITY_PROVIDER_VERIFIED,
                "Verified tenant identity provider " + identityProvider.getId());

        return new TenantIdentityProviderVerificationResponse(
                identityProvider.getId(),
                identityProvider.getStatus(),
                verification.issuer(),
                verification.authorizationEndpoint(),
                verification.tokenEndpoint(),
                verification.jwkSetUri(),
                identityProvider.getVerifiedAt());
    }

    private void requireTenantActor(UUID tenantId, AppUser actor) {
        if (actor == null
                || actor.getTenant() == null
                || !tenantId.equals(actor.getTenant().getId())) {
            throw new AuthenticationFailedException(
                    "Authenticated user does not belong to this tenant");
        }
    }
}
