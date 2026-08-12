package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "authorization_user_role_assignments",
        indexes = {
            @Index(
                    name = "idx_auth_user_role_assignment_user",
                    columnList = "tenant_id,user_id,status"),
            @Index(
                    name = "idx_auth_user_role_assignment_role",
                    columnList = "tenant_id,role_id,status"),
            @Index(
                    name = "idx_auth_user_role_assignment_scope",
                    columnList = "tenant_id,scope_type,scope_key,status"),
            @Index(
                    name = "idx_auth_user_role_assignment_validity",
                    columnList = "tenant_id,valid_from,valid_until")
        })
public class AuthorizationUserRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, updatable = false)
    private AuthorizationRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 40, updatable = false)
    private AuthorizationScopeType scopeType;

    @Column(name = "scope_target_id", updatable = false)
    private UUID scopeTargetId;

    @Column(name = "scope_key", nullable = false, length = 100, updatable = false)
    private String scopeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthorizationUserRoleAssignmentStatus status =
            AuthorizationUserRoleAssignmentStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private AppUser createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AuthorizationUserRoleAssignment() {}

    public AuthorizationUserRoleAssignment(
            Tenant tenant,
            AppUser user,
            AuthorizationRole role,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId,
            String scopeKey,
            Instant validFrom,
            Instant validUntil,
            AppUser createdByUser) {
        this.tenant = tenant;
        this.user = user;
        this.role = role;
        this.scopeType = scopeType;
        this.scopeTargetId = scopeTargetId;
        this.scopeKey = scopeKey;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdByUser = createdByUser;
        this.status = AuthorizationUserRoleAssignmentStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = AuthorizationUserRoleAssignmentStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AppUser getUser() {
        return user;
    }

    public AuthorizationRole getRole() {
        return role;
    }

    public AuthorizationScopeType getScopeType() {
        return scopeType;
    }

    public UUID getScopeTargetId() {
        return scopeTargetId;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public AuthorizationUserRoleAssignmentStatus getStatus() {
        return status;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public AppUser getCreatedByUser() {
        return createdByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setStatus(AuthorizationUserRoleAssignmentStatus status) {
        this.status = status;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }
}
