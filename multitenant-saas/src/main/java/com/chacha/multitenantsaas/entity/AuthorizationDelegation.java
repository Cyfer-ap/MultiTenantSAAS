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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "authorization_delegations",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_authorization_delegation_assignment",
                    columnNames = "delegated_assignment_id")
        },
        indexes = {
            @Index(
                    name = "idx_authorization_delegation_delegator",
                    columnList = "tenant_id,delegator_user_id,status"),
            @Index(
                    name = "idx_authorization_delegation_delegate",
                    columnList = "tenant_id,delegate_user_id,status"),
            @Index(
                    name = "idx_authorization_delegation_parent",
                    columnList = "tenant_id,parent_authority_assignment_id,status")
        })
public class AuthorizationDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegator_user_id", nullable = false, updatable = false)
    private AppUser delegatorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate_user_id", nullable = false, updatable = false)
    private AppUser delegateUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_authority_assignment_id", nullable = false, updatable = false)
    private AuthorizationUserRoleAssignment parentAuthorityAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_assignment_id", nullable = false, updatable = false)
    private AuthorizationUserRoleAssignment delegatedAssignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthorizationDelegationStatus status = AuthorizationDelegationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private AppUser revokedByUser;

    public AuthorizationDelegation() {}

    public AuthorizationDelegation(
            Tenant tenant,
            AppUser delegatorUser,
            AppUser delegateUser,
            AuthorizationUserRoleAssignment parentAuthorityAssignment,
            AuthorizationUserRoleAssignment delegatedAssignment) {
        this.tenant = tenant;
        this.delegatorUser = delegatorUser;
        this.delegateUser = delegateUser;
        this.parentAuthorityAssignment = parentAuthorityAssignment;
        this.delegatedAssignment = delegatedAssignment;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = AuthorizationDelegationStatus.ACTIVE;
        }
    }

    public void revoke(AppUser revokedByUser, Instant revokedAt) {
        this.status = AuthorizationDelegationStatus.REVOKED;
        this.revokedByUser = revokedByUser;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AppUser getDelegatorUser() {
        return delegatorUser;
    }

    public AppUser getDelegateUser() {
        return delegateUser;
    }

    public AuthorizationUserRoleAssignment getParentAuthorityAssignment() {
        return parentAuthorityAssignment;
    }

    public AuthorizationUserRoleAssignment getDelegatedAssignment() {
        return delegatedAssignment;
    }

    public AuthorizationDelegationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public AppUser getRevokedByUser() {
        return revokedByUser;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
