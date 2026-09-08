package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OidcCallbackService {

    private final OidcAuthorizationTransactionStateService stateService;
    private final OidcProviderMetadataService metadataService;
    private final IdentityProviderSecretCipher secretCipher;
    private final OidcTokenExchangeService tokenExchangeService;
    private final OidcIdTokenValidator idTokenValidator;
    private final OidcFederatedIdentityService federatedIdentityService;
    private final OidcSessionHandoffService sessionHandoffService;
    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;

    public OidcCallbackService(
            OidcAuthorizationTransactionStateService stateService,
            OidcProviderMetadataService metadataService,
            IdentityProviderSecretCipher secretCipher,
            OidcTokenExchangeService tokenExchangeService,
            OidcIdTokenValidator idTokenValidator,
            OidcFederatedIdentityService federatedIdentityService,
            OidcSessionHandoffService sessionHandoffService,
            TenantRepository tenantRepository,
            AuditLogService auditLogService) {
        this.stateService = stateService;
        this.metadataService = metadataService;
        this.secretCipher = secretCipher;
        this.tokenExchangeService = tokenExchangeService;
        this.idTokenValidator = idTokenValidator;
        this.federatedIdentityService = federatedIdentityService;
        this.sessionHandoffService = sessionHandoffService;
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
    }

    public OidcSessionHandoffService.IssuedHandoff authenticate(
            String state, String code, String providerError) {
        OidcCallbackTransactionSnapshot transaction = stateService.consume(state);
        Tenant tenant = tenantRepository.findById(transaction.tenantId()).orElse(null);

        try {
            if (providerError != null && !providerError.isBlank()) {
                throw failed();
            }

            OidcProviderMetadata metadata = metadataService.loadAndValidate(transaction.issuerUri());
            String clientSecret = secretCipher.decrypt(transaction.clientSecretCiphertext());
            String pkceVerifier =
                    secretCipher.decryptTransactionSecret(transaction.pkceVerifierCiphertext());
            String idToken =
                    tokenExchangeService.exchange(
                            metadata, transaction.clientId(), clientSecret, code, pkceVerifier);
            OidcVerifiedIdentity identity =
                    idTokenValidator.validate(
                            idToken, metadata, transaction.clientId(), transaction.nonceHash());
            UUID userId = federatedIdentityService.resolveUser(transaction, identity);
            OidcSessionHandoffService.IssuedHandoff handoff =
                    sessionHandoffService.issue(
                            transaction.tenantId(), userId, transaction.persistentSession());

            if (tenant != null) {
                auditLogService.recordSystemSuccess(
                        tenant,
                        AuditAction.IDENTITY_PROVIDER_LOGIN_SUCCESS,
                        "OIDC authentication completed successfully");
            }
            return handoff;
        } catch (RuntimeException exception) {
            if (tenant != null) {
                auditLogService.recordSystemFailure(
                        tenant,
                        AuditAction.IDENTITY_PROVIDER_LOGIN_FAILED,
                        "OIDC authentication failed after trusted state validation");
            }
            throw exception;
        }
    }

    private AuthenticationFailedException failed() {
        return new AuthenticationFailedException("OIDC authentication failed");
    }
}
