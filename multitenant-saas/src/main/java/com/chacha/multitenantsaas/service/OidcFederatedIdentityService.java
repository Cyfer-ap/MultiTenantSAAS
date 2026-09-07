package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.TenantFederatedIdentity;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantFederatedIdentityRepository;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OidcFederatedIdentityService {

    private final TenantIdentityProviderRepository identityProviderRepository;
    private final TenantFederatedIdentityRepository federatedIdentityRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    public OidcFederatedIdentityService(
            TenantIdentityProviderRepository identityProviderRepository,
            TenantFederatedIdentityRepository federatedIdentityRepository,
            AppUserRepository appUserRepository,
            AuditLogService auditLogService) {
        this.identityProviderRepository = identityProviderRepository;
        this.federatedIdentityRepository = federatedIdentityRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UUID resolveUser(
            OidcCallbackTransactionSnapshot transaction, OidcVerifiedIdentity identity) {
        TenantIdentityProvider provider =
                identityProviderRepository
                        .findByTenant_Id(transaction.tenantId())
                        .orElseThrow(this::failed);
        requireCurrentProvider(provider, transaction);

        String issuerHash = OidcSecuritySupport.sha256Hex(identity.issuer());
        Optional<TenantFederatedIdentity> existing =
                federatedIdentityRepository.findByTenant_IdAndIssuerHashAndSubject(
                        transaction.tenantId(), issuerHash, identity.subject());
        Instant now = Instant.now();

        if (existing.isPresent()) {
            TenantFederatedIdentity link = existing.get();
            if (!link.getIdentityProvider().getId().equals(provider.getId())) {
                throw failed();
            }
            AppUser user = requireActiveUser(transaction.tenantId(), link.getUser().getId());
            link.recordLogin(now);
            federatedIdentityRepository.save(link);
            return user.getId();
        }

        if (!identity.emailVerified()
                || identity.email() == null
                || identity.email().isBlank()
                || identity.email().length() > 150) {
            throw failed();
        }

        String normalizedEmail = identity.email().trim().toLowerCase(Locale.ROOT);
        AppUser user =
                appUserRepository
                        .findByTenantIdAndEmailForUpdate(transaction.tenantId(), normalizedEmail)
                        .orElseThrow(this::failed);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw failed();
        }

        Optional<TenantFederatedIdentity> existingUserLink =
                federatedIdentityRepository.findByIdentityProvider_IdAndUser_IdAndIssuerHash(
                        provider.getId(), user.getId(), issuerHash);
        if (existingUserLink.isPresent()) {
            throw failed();
        }

        TenantFederatedIdentity link =
                new TenantFederatedIdentity(
                        provider.getTenant(),
                        provider,
                        user,
                        identity.issuer(),
                        issuerHash,
                        identity.subject(),
                        normalizedEmail,
                        now);
        try {
            federatedIdentityRepository.saveAndFlush(link);
        } catch (DataIntegrityViolationException exception) {
            throw failed();
        }

        auditLogService.recordSuccess(
                provider.getTenant(),
                user,
                user,
                AuditAction.IDENTITY_PROVIDER_IDENTITY_LINKED,
                "Linked verified OIDC identity to tenant user");
        return user.getId();
    }

    private void requireCurrentProvider(
            TenantIdentityProvider provider, OidcCallbackTransactionSnapshot transaction) {
        if (provider.getTenant().getStatus() != TenantStatus.ACTIVE
                || provider.getStatus() != TenantIdentityProviderStatus.VERIFIED
                || !provider.getId().equals(transaction.identityProviderId())
                || provider.getVersion() != transaction.identityProviderVersion()
                || !provider.getIssuerUri().equals(transaction.issuerUri())) {
            throw failed();
        }
    }

    private AppUser requireActiveUser(UUID tenantId, UUID userId) {
        AppUser user =
                appUserRepository
                        .findByTenantIdAndIdForUpdate(tenantId, userId)
                        .orElseThrow(this::failed);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw failed();
        }
        return user;
    }

    private AuthenticationFailedException failed() {
        return new AuthenticationFailedException("OIDC authentication failed");
    }
}
