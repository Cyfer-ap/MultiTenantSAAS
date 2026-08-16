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
        name = "authorization_permissions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_authorization_permission_catalog_code",
                    columnNames = {"catalog_key", "code"})
        },
        indexes = {
            @Index(
                    name = "idx_authorization_permission_source_status",
                    columnList = "source,status"),
            @Index(
                    name = "idx_authorization_permission_tenant_status",
                    columnList = "tenant_id,status"),
            @Index(name = "idx_authorization_permission_category", columnList = "category")
        })
public class AuthorizationPermission {

    public static final String PLATFORM_CATALOG_KEY = "PLATFORM";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "catalog_key", nullable = false, length = 64, updatable = false)
    private String catalogKey;

    @Column(nullable = false, length = 120, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 60)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuthorizationPermissionSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthorizationPermissionStatus status = AuthorizationPermissionStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public AuthorizationPermission() {}

    public AuthorizationPermission(
            Tenant tenant, String code, String name, String description, String category) {
        this.tenant = tenant;
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.source =
                tenant == null
                        ? AuthorizationPermissionSource.PLATFORM
                        : AuthorizationPermissionSource.TENANT;

        synchronizeCatalogIdentity();
    }

    @PrePersist
    protected void onCreate() {
        synchronizeCatalogIdentity();

        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = AuthorizationPermissionStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        synchronizeCatalogIdentity();
        this.updatedAt = Instant.now();
    }

    private void synchronizeCatalogIdentity() {
        if (source == AuthorizationPermissionSource.PLATFORM) {
            this.tenant = null;
            this.catalogKey = PLATFORM_CATALOG_KEY;
            return;
        }

        if (source == AuthorizationPermissionSource.TENANT) {
            if (tenant == null || tenant.getId() == null) {
                throw new IllegalStateException(
                        "Tenant permission requires " + "a persisted tenant.");
            }

            this.catalogKey = tenant.getId().toString();

            return;
        }

        if (tenant == null) {
            this.source = AuthorizationPermissionSource.PLATFORM;
            this.catalogKey = PLATFORM_CATALOG_KEY;
        } else {
            if (tenant.getId() == null) {
                throw new IllegalStateException(
                        "Tenant permission requires " + "a persisted tenant.");
            }

            this.source = AuthorizationPermissionSource.TENANT;
            this.catalogKey = tenant.getId().toString();
        }
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getCatalogKey() {
        return catalogKey;
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

    public String getCategory() {
        return category;
    }

    public AuthorizationPermissionSource getSource() {
        return source;
    }

    public AuthorizationPermissionStatus getStatus() {
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

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStatus(AuthorizationPermissionStatus status) {
        this.status = status;
    }
}
