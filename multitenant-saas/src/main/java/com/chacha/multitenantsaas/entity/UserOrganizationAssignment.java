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
        name = "user_organization_assignments",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_user_org_assignment_tenant_id",
                    columnNames = {"tenant_id", "id"})
        },
        indexes = {
            @Index(name = "idx_user_org_assignment_user", columnList = "tenant_id,user_id,status"),
            @Index(
                    name = "idx_user_org_assignment_unit",
                    columnList = "tenant_id,organizational_unit_id,status"),
            @Index(
                    name = "idx_user_org_assignment_primary",
                    columnList = "tenant_id,user_id,primary_assignment,status"),
            @Index(
                    name = "idx_user_org_assignment_reports_to",
                    columnList = "tenant_id,reports_to_assignment_id")
        })
public class UserOrganizationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizational_unit_id", nullable = false)
    private OrganizationalUnit organizationalUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_assignment_id")
    private UserOrganizationAssignment reportsToAssignment;

    @Column(name = "position_title", length = 150)
    private String positionTitle;

    @Column(name = "primary_assignment", nullable = false)
    private boolean primaryAssignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationAssignmentStatus status = OrganizationAssignmentStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserOrganizationAssignment() {}

    public UserOrganizationAssignment(
            Tenant tenant,
            AppUser user,
            OrganizationalUnit organizationalUnit,
            UserOrganizationAssignment reportsToAssignment,
            String positionTitle,
            boolean primaryAssignment,
            Instant validFrom,
            Instant validUntil,
            AppUser createdByUser) {
        this.tenant = tenant;
        this.user = user;
        this.organizationalUnit = organizationalUnit;
        this.reportsToAssignment = reportsToAssignment;
        this.positionTitle = positionTitle;
        this.primaryAssignment = primaryAssignment;
        this.status = OrganizationAssignmentStatus.ACTIVE;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdByUser = createdByUser;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (validFrom == null) {
            validFrom = now;
        }

        if (status == null) {
            status = OrganizationAssignmentStatus.ACTIVE;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
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

    public OrganizationalUnit getOrganizationalUnit() {
        return organizationalUnit;
    }

    public UserOrganizationAssignment getReportsToAssignment() {
        return reportsToAssignment;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public boolean isPrimaryAssignment() {
        return primaryAssignment;
    }

    public OrganizationAssignmentStatus getStatus() {
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

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public void setOrganizationalUnit(OrganizationalUnit organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    public void setReportsToAssignment(UserOrganizationAssignment reportsToAssignment) {
        this.reportsToAssignment = reportsToAssignment;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public void setPrimaryAssignment(boolean primaryAssignment) {
        this.primaryAssignment = primaryAssignment;
    }

    public void setStatus(OrganizationAssignmentStatus status) {
        this.status = status;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public void setCreatedByUser(AppUser createdByUser) {
        this.createdByUser = createdByUser;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
