package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.OidcAuthorizationTransaction;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.OidcAuthorizationTransactionRepository;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OidcAuthorizationTransactionStateService {

    private final TenantIdentityProviderRepository identityProviderRepository;
    private final OidcAuthorizationTransactionRepository transactionRepository;

    public OidcAuthorizationTransactionStateService(
            TenantIdentityProviderRepository identityProviderRepository,
            OidcAuthorizationTransactionRepository transactionRepository) {
        this.identityProviderRepository = identityProviderRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public OidcProviderRuntimeSnapshot loadVerifiedProvider(UUID tenantId) {
        TenantIdentityProvider provider = requireVerifiedProvider(tenantId);
        return snapshot(provider);
    }

    @Transactional
    public Instant createTransaction(
            OidcProviderRuntimeSnapshot expectedProvider,
            String stateHash,
            String nonceHash,
            String pkceVerifierCiphertext,
            boolean persistentSession,
            Instant createdAt,
            Instant expiresAt) {
        TenantIdentityProvider current = requireVerifiedProvider(expectedProvider.tenantId());
        requireUnchangedProvider(current, expectedProvider);

        OidcAuthorizationTransaction transaction =
                new OidcAuthorizationTransaction(
                        current.getTenant(),
                        current,
                        current.getVersion(),
                        stateHash,
                        nonceHash,
                        pkceVerifierCiphertext,
                        persistentSession,
                        createdAt,
                        expiresAt);
        transactionRepository.save(transaction);
        return expiresAt;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public OidcCallbackTransactionSnapshot consume(String rawState) {
        if (rawState == null || rawState.isBlank() || rawState.length() > 512) {
            throw invalidTransaction();
        }

        String stateHash = OidcSecuritySupport.sha256Hex(rawState);
        OidcAuthorizationTransaction transaction =
                transactionRepository
                        .findByStateHashForUpdate(stateHash)
                        .orElseThrow(this::invalidTransaction);

        if (transaction.isConsumed()) {
            throw invalidTransaction();
        }

        Instant now = Instant.now();
        transaction.consume(now);
        transactionRepository.save(transaction);

        if (transaction.isExpired(now)) {
            throw invalidTransaction();
        }

        TenantIdentityProvider provider = transaction.getIdentityProvider();
        if (transaction.getTenant().getStatus() != TenantStatus.ACTIVE
                || provider.getStatus() != TenantIdentityProviderStatus.VERIFIED
                || provider.getProtocol() != IdentityProviderProtocol.OIDC
                || provider.getVersion() != transaction.getIdentityProviderVersion()) {
            throw invalidTransaction();
        }

        return new OidcCallbackTransactionSnapshot(
                transaction.getTenant().getId(),
                provider.getId(),
                provider.getVersion(),
                provider.getIssuerUri(),
                provider.getClientId(),
                provider.getClientSecretCiphertext(),
                provider.getScopes(),
                provider.getDisplayName(),
                transaction.getNonceHash(),
                transaction.getPkceVerifierCiphertext(),
                transaction.isPersistentSession());
    }

    private TenantIdentityProvider requireVerifiedProvider(UUID tenantId) {
        TenantIdentityProvider provider =
                identityProviderRepository.findByTenant_Id(tenantId).orElseThrow(this::unavailable);
        if (provider.getTenant().getStatus() != TenantStatus.ACTIVE
                || provider.getStatus() != TenantIdentityProviderStatus.VERIFIED
                || provider.getProtocol() != IdentityProviderProtocol.OIDC) {
            throw unavailable();
        }
        return provider;
    }

    private OidcProviderRuntimeSnapshot snapshot(TenantIdentityProvider provider) {
        return new OidcProviderRuntimeSnapshot(
                provider.getTenant().getId(),
                provider.getId(),
                provider.getVersion(),
                provider.getIssuerUri(),
                provider.getClientId(),
                provider.getClientSecretCiphertext(),
                provider.getScopes(),
                provider.getDisplayName());
    }

    private void requireUnchangedProvider(
            TenantIdentityProvider current, OidcProviderRuntimeSnapshot expected) {
        if (!current.getId().equals(expected.identityProviderId())
                || current.getVersion() != expected.identityProviderVersion()) {
            throw new AuthenticationFailedException(
                    "OIDC provider configuration changed; restart authentication");
        }
    }

    private AuthenticationFailedException unavailable() {
        return new AuthenticationFailedException("OIDC login is unavailable for this tenant");
    }

    private AuthenticationFailedException invalidTransaction() {
        return new AuthenticationFailedException(
                "Invalid or expired OIDC authentication transaction");
    }
}
