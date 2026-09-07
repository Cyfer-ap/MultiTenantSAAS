package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.OidcAuthorizationTransaction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.OidcAuthorizationTransactionRepository;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OidcAuthorizationTransactionStateServiceTest {

    @Mock private TenantIdentityProviderRepository identityProviderRepository;
    @Mock private OidcAuthorizationTransactionRepository transactionRepository;
    @Mock private OidcAuthorizationTransaction transaction;
    @Mock private TenantIdentityProvider provider;
    @Mock private Tenant tenant;

    private OidcAuthorizationTransactionStateService service;

    @BeforeEach
    void setUp() {
        service =
                new OidcAuthorizationTransactionStateService(
                        identityProviderRepository, transactionRepository);
    }

    @Test
    void rejectsReplayOfAlreadyConsumedStateWithoutMutatingTransaction() {
        String state = "one-time-state";
        when(transactionRepository.findByStateHashForUpdate(OidcSecuritySupport.sha256Hex(state)))
                .thenReturn(Optional.of(transaction));
        when(transaction.isConsumed()).thenReturn(true);

        assertThatThrownBy(() -> service.consume(state))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid or expired OIDC authentication transaction");

        verify(transaction, never()).consume(org.mockito.ArgumentMatchers.any(Instant.class));
        verify(transactionRepository, never()).save(transaction);
    }

    @Test
    void consumesExpiredStateBeforeRejectingItSoItCannotBeReplayed() {
        String state = "expired-state";
        when(transactionRepository.findByStateHashForUpdate(OidcSecuritySupport.sha256Hex(state)))
                .thenReturn(Optional.of(transaction));
        when(transaction.isConsumed()).thenReturn(false);
        when(transaction.isExpired(org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(true);

        assertThatThrownBy(() -> service.consume(state))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid or expired OIDC authentication transaction");

        verify(transaction).consume(org.mockito.ArgumentMatchers.any(Instant.class));
        verify(transactionRepository).save(transaction);
    }

    @Test
    void returnsTenantBoundSnapshotOnlyForCurrentVerifiedProvider() {
        String state = "valid-state";
        UUID tenantId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        when(transactionRepository.findByStateHashForUpdate(OidcSecuritySupport.sha256Hex(state)))
                .thenReturn(Optional.of(transaction));
        when(transaction.isConsumed()).thenReturn(false);
        when(transaction.isExpired(org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(false);
        when(transaction.getTenant()).thenReturn(tenant);
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(tenant.getId()).thenReturn(tenantId);
        when(transaction.getIdentityProvider()).thenReturn(provider);
        when(transaction.getIdentityProviderVersion()).thenReturn(7L);
        when(provider.getStatus()).thenReturn(TenantIdentityProviderStatus.VERIFIED);
        when(provider.getProtocol()).thenReturn(IdentityProviderProtocol.OIDC);
        when(provider.getVersion()).thenReturn(7L);
        when(provider.getId()).thenReturn(providerId);
        when(provider.getIssuerUri()).thenReturn("https://idp.example.com");
        when(provider.getClientId()).thenReturn("client-id");
        when(provider.getClientSecretCiphertext()).thenReturn("ciphertext");
        when(provider.getScopes()).thenReturn(Set.of("openid", "email"));
        when(provider.getDisplayName()).thenReturn("Enterprise IdP");
        when(transaction.getNonceHash()).thenReturn("nonce-hash");
        when(transaction.getPkceVerifierCiphertext()).thenReturn("pkce-ciphertext");
        when(transaction.isPersistentSession()).thenReturn(true);

        OidcCallbackTransactionSnapshot snapshot = service.consume(state);

        assertThat(snapshot.tenantId()).isEqualTo(tenantId);
        assertThat(snapshot.identityProviderId()).isEqualTo(providerId);
        assertThat(snapshot.identityProviderVersion()).isEqualTo(7L);
        assertThat(snapshot.persistentSession()).isTrue();
        verify(transaction).consume(org.mockito.ArgumentMatchers.any(Instant.class));
        verify(transactionRepository).save(transaction);
    }
}
