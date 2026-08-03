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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "authorization_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_authorization_role_tenant_code",
                        columnNames = {
                                "tenant_id",
                                "code"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_authorization_role_tenant_status",
                        columnList = "tenant_id,status"
                ),
                @Index(
                        name = "idx_authorization_role_tenant_source",
                        columnList = "tenant_id,source"
                )
        }
)
public class AuthorizationRole {

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

    @Column(
            nullable = false,
            length = 60,
            updatable = false
    )
    private String code;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30,
            updatable = false
    )
    private AuthorizationRoleSource source;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private AuthorizationRoleStatus status =
            AuthorizationRoleStatus.ACTIVE;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    public AuthorizationRole() {
    }

    public AuthorizationRole(
            Tenant tenant,
            String code,
            String name,
            String description,
            AuthorizationRoleSource source
    ) {
        this.tenant = tenant;
        this.code = code;
        this.name = name;
        this.description = description;
        this.source = source;
        this.status = AuthorizationRoleStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = AuthorizationRoleStatus.ACTIVE;
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AuthorizationRoleSource getSource() {
        return source;
    }

    public AuthorizationRoleStatus getStatus() {
        return status;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(
            AuthorizationRoleStatus status
    ) {
        this.status = status;
    }
}