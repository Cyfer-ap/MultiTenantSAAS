package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_identity_providers",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tenant_identity_provider_tenant",
                    columnNames = "tenant_id")
        },
        indexes = {
            @Index(name = "idx_tenant_identity_provider_status", columnList = "status, updated_at")
        })
public class TenantIdentityProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdentityProviderProtocol protocol;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "issuer_uri", nullable = false, length = 2048)
    private String issuerUri;

    @Column(name = "client_id", nullable = false, length = 512)
    private String clientId;

    @Column(name = "client_secret_ciphertext", nullable = false, length = 4096)
    private String clientSecretCiphertext;

    @Column(name = "client_secret_hint", nullable = false, length = 32)
    private String clientSecretHint;

    @Column(name = "secret_version", nullable = false)
    private int secretVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "tenant_identity_provider_scopes",
            joinColumns = @JoinColumn(name = "identity_provider_id"))
    @Column(name = "scope", nullable = false, length = 100)
    private Set<String> scopes = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantIdentityProviderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "sso_mode", nullable = false, length = 20)
    private TenantSsoMode ssoMode = TenantSsoMode.OPTIONAL;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_user_id", nullable = false)
    private AppUser updatedByUser;

    @Column(name = "secret_rotated_at", nullable = false)
    private Instant secretRotatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public TenantIdentityProvider() {}

    public TenantIdentityProvider(
            Tenant tenant,
            IdentityProviderProtocol protocol,
            String displayName,
            String issuerUri,
            String clientId,
            String clientSecretCiphertext,
            String clientSecretHint,
            Set<String> scopes,
            AppUser actor,
            Instant now) {
        this.tenant = tenant;
        this.protocol = protocol;
        this.displayName = displayName;
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.clientSecretCiphertext = clientSecretCiphertext;
        this.clientSecretHint = clientSecretHint;
        this.secretVersion = 1;
        this.scopes = copyScopes(scopes);
        this.status = TenantIdentityProviderStatus.DRAFT;
        this.ssoMode = TenantSsoMode.OPTIONAL;
        this.createdByUser = actor;
        this.updatedByUser = actor;
        this.secretRotatedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String displayName,
            String issuerUri,
            String clientId,
            Set<String> scopes,
            AppUser actor,
            Instant now) {
        this.displayName = displayName;
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.scopes = copyScopes(scopes);
        invalidateVerification();
        this.updatedByUser = actor;
        this.updatedAt = now;
    }

    public void rotateClientSecret(String ciphertext, String hint, AppUser actor, Instant now) {
        this.clientSecretCiphertext = ciphertext;
        this.clientSecretHint = hint;
        this.secretVersion++;
        this.secretRotatedAt = now;
        invalidateVerification();
        this.updatedByUser = actor;
        this.updatedAt = now;
    }

    public void markVerified(AppUser actor, Instant now) {
        if (status == TenantIdentityProviderStatus.DISABLED) {
            throw new IllegalStateException(
                    "Disabled identity-provider configuration cannot be verified");
        }
        status = TenantIdentityProviderStatus.VERIFIED;
        verifiedAt = now;
        disabledAt = null;
        updatedByUser = actor;
        updatedAt = now;
    }

    public void updateSsoMode(TenantSsoMode mode, AppUser actor, Instant now) {
        TenantSsoMode requestedMode = Objects.requireNonNull(mode, "SSO mode must not be null");
        if (status == TenantIdentityProviderStatus.DISABLED) {
            throw new IllegalStateException("Disabled identity-provider configuration has no SSO policy");
        }
        if (requestedMode == TenantSsoMode.REQUIRED
                && status != TenantIdentityProviderStatus.VERIFIED) {
            throw new IllegalStateException("SSO can only be required for a verified identity provider");
        }
        ssoMode = requestedMode;
        updatedByUser = actor;
        updatedAt = now;
    }

    public void disable(AppUser actor, Instant now) {
        ssoMode = TenantSsoMode.OPTIONAL;
        if (status != TenantIdentityProviderStatus.DISABLED) {
            status = TenantIdentityProviderStatus.DISABLED;
            disabledAt = now;
            verifiedAt = null;
            updatedByUser = actor;
            updatedAt = now;
        }
    }

    private void invalidateVerification() {
        ssoMode = TenantSsoMode.OPTIONAL;
        if (status == TenantIdentityProviderStatus.VERIFIED) {
            status = TenantIdentityProviderStatus.DRAFT;
            verifiedAt = null;
        }
    }

    private static Set<String> copyScopes(Set<String> values) {
        return values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public IdentityProviderProtocol getProtocol() {
        return protocol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecretCiphertext() {
        return clientSecretCiphertext;
    }

    public String getClientSecretHint() {
        return clientSecretHint;
    }

    public int getSecretVersion() {
        return secretVersion;
    }

    public Set<String> getScopes() {
        return Set.copyOf(scopes);
    }

    public TenantIdentityProviderStatus getStatus() {
        return status;
    }

    public TenantSsoMode getSsoMode() {
        return ssoMode;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public AppUser getCreatedByUser() {
        return createdByUser;
    }

    public AppUser getUpdatedByUser() {
        return updatedByUser;
    }

    public Instant getSecretRotatedAt() {
        return secretRotatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
