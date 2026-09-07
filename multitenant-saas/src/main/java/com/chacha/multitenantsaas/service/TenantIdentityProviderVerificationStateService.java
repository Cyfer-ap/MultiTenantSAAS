package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderVerificationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.IdentityProviderVerificationException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantIdentityProviderVerificationStateService {

    private final TenantIdentityProviderRepository identityProviderRepository;
    private final AuditLogService auditLogService;

    public TenantIdentityProviderVerificationStateService(
            TenantIdentityProviderRepository identityProviderRepository,
            AuditLogService auditLogService) {
        this.identityProviderRepository = identityProviderRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public TenantIdentityProviderVerificationSnapshot loadSnapshot(UUID tenantId, AppUser actor) {
        TenantIdentityProvider identityProvider = requireConfiguration(tenantId);
        validateMutableVerificationState(tenantId, actor, identityProvider);

        return new TenantIdentityProviderVerificationSnapshot(
                identityProvider.getId(),
                identityProvider.getVersion(),
                new OidcProviderVerificationInput(
                        tenantId,
                        identityProvider.getProtocol(),
                        identityProvider.getIssuerUri(),
                        identityProvider.getClientId(),
                        identityProvider.getClientSecretCiphertext(),
                        identityProvider.getScopes(),
                        identityProvider.getDisplayName()));
    }

    @Transactional
    public TenantIdentityProviderVerificationResponse markVerified(
            UUID tenantId,
            UUID identityProviderId,
            long expectedVersion,
            AppUser actor,
            OidcProviderVerificationResult verification) {
        TenantIdentityProvider identityProvider = requireConfiguration(tenantId);
        validateMutableVerificationState(tenantId, actor, identityProvider);

        if (!identityProviderId.equals(identityProvider.getId())
                || expectedVersion != identityProvider.getVersion()) {
            throw new IdentityProviderVerificationException(
                    "Identity-provider configuration changed during verification; verify it again");
        }

        Instant now = Instant.now();
        identityProvider.markVerified(actor, now);
        identityProviderRepository.save(identityProvider);

        Tenant tenant = identityProvider.getTenant();
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

    private TenantIdentityProvider requireConfiguration(UUID tenantId) {
        return identityProviderRepository
                .findByTenant_Id(tenantId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant identity-provider configuration not found"));
    }

    private void validateMutableVerificationState(
            UUID tenantId, AppUser actor, TenantIdentityProvider identityProvider) {
        Tenant tenant = identityProvider.getTenant();
        if (actor == null
                || actor.getTenant() == null
                || !tenantId.equals(actor.getTenant().getId())) {
            throw new AuthenticationFailedException(
                    "Authenticated user does not belong to this tenant");
        }
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Identity federation can only be verified for active tenants");
        }
        if (identityProvider.getStatus() == TenantIdentityProviderStatus.DISABLED) {
            throw new IllegalArgumentException(
                    "Disabled identity-provider configuration cannot be verified");
        }
    }
}
