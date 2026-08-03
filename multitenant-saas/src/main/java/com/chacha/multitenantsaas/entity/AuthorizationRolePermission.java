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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "authorization_role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_authorization_role_permission",
                        columnNames = {
                                "role_id",
                                "permission_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_authorization_role_permission_role",
                        columnList = "tenant_id,role_id"
                ),
                @Index(
                        name = "idx_authorization_role_permission_permission",
                        columnList = "permission_id"
                )
        }
)
public class AuthorizationRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tenant_id",
            nullable = false,
            updatable = false
    )
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            updatable = false
    )
    private AuthorizationRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "permission_id",
            nullable = false,
            updatable = false
    )
    private AuthorizationPermission permission;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    public AuthorizationRolePermission() {
    }

    public AuthorizationRolePermission(
            Tenant tenant,
            AuthorizationRole role,
            AuthorizationPermission permission
    ) {
        this.tenant = tenant;
        this.role = role;
        this.permission = permission;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AuthorizationRole getRole() {
        return role;
    }

    public AuthorizationPermission getPermission() {
        return permission;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}