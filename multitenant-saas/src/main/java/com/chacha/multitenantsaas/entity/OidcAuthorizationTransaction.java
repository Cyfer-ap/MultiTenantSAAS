package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "oidc_authorization_transactions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_oidc_authorization_transaction_state",
                        columnNames = "state_hash"),
        indexes = {
            @Index(
                    name = "idx_oidc_authorization_transaction_expiry",
                    columnList = "expires_at, consumed_at"),
            @Index(
                    name = "idx_oidc_authorization_transaction_provider",
                    columnList = "tenant_id, identity_provider_id")
        })
public class OidcAuthorizationTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_provider_id", nullable = false)
    private TenantIdentityProvider identityProvider;

    @Column(name = "identity_provider_version", nullable = false)
    private long identityProviderVersion;

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(name = "nonce_hash", nullable = false, length = 64)
    private String nonceHash;

    @Column(name = "pkce_verifier_ciphertext", nullable = false, length = 4096)
    private String pkceVerifierCiphertext;

    @Column(name = "persistent_session", nullable = false)
    private boolean persistentSession;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public OidcAuthorizationTransaction() {}

    public OidcAuthorizationTransaction(
            Tenant tenant,
            TenantIdentityProvider identityProvider,
            long identityProviderVersion,
            String stateHash,
            String nonceHash,
            String pkceVerifierCiphertext,
            boolean persistentSession,
            Instant createdAt,
            Instant expiresAt) {
        this.tenant = tenant;
        this.identityProvider = identityProvider;
        this.identityProviderVersion = identityProviderVersion;
        this.stateHash = stateHash;
        this.nonceHash = nonceHash;
        this.pkceVerifierCiphertext = pkceVerifierCiphertext;
        this.persistentSession = persistentSession;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void consume(Instant now) {
        if (consumedAt != null) {
            throw new IllegalStateException("OIDC authorization transaction is already consumed");
        }
        consumedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public TenantIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    public long getIdentityProviderVersion() {
        return identityProviderVersion;
    }

    public String getStateHash() {
        return stateHash;
    }

    public String getNonceHash() {
        return nonceHash;
    }

    public String getPkceVerifierCiphertext() {
        return pkceVerifierCiphertext;
    }

    public boolean isPersistentSession() {
        return persistentSession;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }
}
