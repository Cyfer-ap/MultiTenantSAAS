package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
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

    public OidcCallbackService(
            OidcAuthorizationTransactionStateService stateService,
            OidcProviderMetadataService metadataService,
            IdentityProviderSecretCipher secretCipher,
            OidcTokenExchangeService tokenExchangeService,
            OidcIdTokenValidator idTokenValidator,
            OidcFederatedIdentityService federatedIdentityService,
            OidcSessionHandoffService sessionHandoffService) {
        this.stateService = stateService;
        this.metadataService = metadataService;
        this.secretCipher = secretCipher;
        this.tokenExchangeService = tokenExchangeService;
        this.idTokenValidator = idTokenValidator;
        this.federatedIdentityService = federatedIdentityService;
        this.sessionHandoffService = sessionHandoffService;
    }

    public OidcSessionHandoffService.IssuedHandoff authenticate(
            String state, String code, String providerError) {
        OidcCallbackTransactionSnapshot transaction = stateService.consume(state);
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
        return sessionHandoffService.issue(
                transaction.tenantId(), userId, transaction.persistentSession());
    }

    private AuthenticationFailedException failed() {
        return new AuthenticationFailedException("OIDC authentication failed");
    }
}
