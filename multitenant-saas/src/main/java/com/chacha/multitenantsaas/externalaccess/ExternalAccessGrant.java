package com.chacha.multitenantsaas.externalaccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_access_grants")
public class ExternalAccessGrant {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;

    @Column(name = "guest_name", nullable = false, length = 150)
    private String guestName;

    @Column(name = "guest_email", nullable = false, length = 150)
    private String guestEmail;

    @Column(name = "invitation_token_hash", nullable = false, length = 64)
    private String invitationTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExternalAccessGrant() {}

    public ExternalAccessGrant(
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            String guestName,
            String guestEmail,
            String invitationTokenHash,
            Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.createdByUserId = createdByUserId;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.invitationTokenHash = invitationTokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isAvailable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        if (!isAvailable(now) || acceptedAt != null) {
            throw new IllegalArgumentException("Guest invitation is no longer available");
        }
        acceptedAt = now;
    }

    public void revoke(UUID actorUserId, Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
            revokedByUserId = actorUserId;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public UUID getRevokedByUserId() {
        return revokedByUserId;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getInvitationTokenHash() {
        return invitationTokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
