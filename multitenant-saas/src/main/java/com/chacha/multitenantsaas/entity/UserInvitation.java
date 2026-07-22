package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_invitations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_invitation_token_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(name = "idx_user_invitation_tenant", columnList = "tenant_id"),
                @Index(name = "idx_user_invitation_email", columnList = "tenant_id,email"),
                @Index(name = "idx_user_invitation_status", columnList = "status"),
                @Index(name = "idx_user_invitation_expires_at", columnList = "expires_at")
        }
)
public class UserInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id")
    private AppUser invitedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_system_admin_id")
    private SystemAdmin invitedBySystemAdmin;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserInvitationStatus status = UserInvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public UserInvitation() {
    }

    public UserInvitation(
            Tenant tenant,
            AppUser invitedByUser,
            SystemAdmin invitedBySystemAdmin,
            String fullName,
            String email,
            UserRole role,
            String tokenHash,
            Instant expiresAt
    ) {
        this.tenant = tenant;
        this.invitedByUser = invitedByUser;
        this.invitedBySystemAdmin = invitedBySystemAdmin;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.status = UserInvitationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == UserInvitationStatus.PENDING && !isExpired();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AppUser getInvitedByUser() {
        return invitedByUser;
    }

    public SystemAdmin getInvitedBySystemAdmin() {
        return invitedBySystemAdmin;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UserInvitationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setStatus(UserInvitationStatus status) {
        this.status = status;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}