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
        name = "organizational_units",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_org_unit_tenant_code",
                    columnNames = {"tenant_id", "code"}),
            @UniqueConstraint(
                    name = "uk_org_unit_tenant_id",
                    columnNames = {"tenant_id", "id"})
        },
        indexes = {
            @Index(name = "idx_org_unit_tenant", columnList = "tenant_id"),
            @Index(name = "idx_org_unit_tenant_parent", columnList = "tenant_id,parent_unit_id"),
            @Index(name = "idx_org_unit_tenant_status", columnList = "tenant_id,status"),
            @Index(name = "idx_org_unit_tenant_type", columnList = "tenant_id,type")
        })
public class OrganizationalUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    private OrganizationalUnit parentUnit;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationalUnitType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationalUnitStatus status = OrganizationalUnitStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public OrganizationalUnit() {}

    public OrganizationalUnit(
            Tenant tenant,
            OrganizationalUnit parentUnit,
            String name,
            String code,
            OrganizationalUnitType type) {
        this.tenant = tenant;
        this.parentUnit = parentUnit;
        this.name = name;
        this.code = code;
        this.type = type;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
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

    public OrganizationalUnit getParentUnit() {
        return parentUnit;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public OrganizationalUnitType getType() {
        return type;
    }

    public OrganizationalUnitStatus getStatus() {
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

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setParentUnit(OrganizationalUnit parentUnit) {
        this.parentUnit = parentUnit;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setType(OrganizationalUnitType type) {
        this.type = type;
    }

    public void setStatus(OrganizationalUnitStatus status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
