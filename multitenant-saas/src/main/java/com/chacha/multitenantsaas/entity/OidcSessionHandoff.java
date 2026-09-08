package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "oidc_session_handoffs",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_oidc_session_handoff_code_hash", columnNames = "code_hash")
        })
public class OidcSessionHandoff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "persistent_session", nullable = false)
    private boolean persistentSession;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected OidcSessionHandoff() {}

    public OidcSessionHandoff(
            String codeHash,
            UUID tenantId,
            UUID userId,
            boolean persistentSession,
            Instant createdAt,
            Instant expiresAt) {
        this.codeHash = codeHash;
        this.tenantId = tenantId;
        this.userId = userId;
        this.persistentSession = persistentSession;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void consume(Instant now) {
        if (consumedAt != null || now == null || !now.isBefore(expiresAt)) {
            throw new IllegalStateException("OIDC session handoff is invalid or expired");
        }
        consumedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
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
