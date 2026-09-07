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
        name = "tenant_federated_identities",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tenant_federated_identity_subject",
                    columnNames = {"tenant_id", "issuer_hash", "subject"}),
            @UniqueConstraint(
                    name = "uk_tenant_federated_identity_user_provider",
                    columnNames = {"identity_provider_id", "user_id", "issuer_hash"})
        },
        indexes = {
            @Index(
                    name = "idx_tenant_federated_identity_user",
                    columnList = "tenant_id, user_id"),
            @Index(
                    name = "idx_tenant_federated_identity_provider",
                    columnList = "identity_provider_id, last_login_at")
        })
public class TenantFederatedIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_provider_id", nullable = false)
    private TenantIdentityProvider identityProvider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 2048)
    private String issuer;

    @Column(name = "issuer_hash", nullable = false, length = 64)
    private String issuerHash;

    @Column(nullable = false, length = 512)
    private String subject;

    @Column(name = "email_at_link", nullable = false, length = 150)
    private String emailAtLink;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    public TenantFederatedIdentity() {}

    public TenantFederatedIdentity(
            Tenant tenant,
            TenantIdentityProvider identityProvider,
            AppUser user,
            String issuer,
            String issuerHash,
            String subject,
            String emailAtLink,
            Instant now) {
        this.tenant = tenant;
        this.identityProvider = identityProvider;
        this.user = user;
        this.issuer = issuer;
        this.issuerHash = issuerHash;
        this.subject = subject;
        this.emailAtLink = emailAtLink;
        this.linkedAt = now;
        this.lastLoginAt = now;
    }

    public void recordLogin(Instant now) {
        this.lastLoginAt = now;
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

    public AppUser getUser() {
        return user;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getIssuerHash() {
        return issuerHash;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmailAtLink() {
        return emailAtLink;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
