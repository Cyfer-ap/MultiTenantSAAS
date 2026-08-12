package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "organizational_unit_closure",
        indexes = {
            @Index(
                    name = "idx_org_closure_ancestor",
                    columnList = "tenant_id,ancestor_unit_id,depth"),
            @Index(
                    name = "idx_org_closure_descendant",
                    columnList = "tenant_id,descendant_unit_id,depth")
        })
public class OrganizationalUnitClosure {

    @EmbeddedId private OrganizationalUnitClosureId id;

    @MapsId("tenantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @MapsId("ancestorUnitId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ancestor_unit_id", nullable = false)
    private OrganizationalUnit ancestorUnit;

    @MapsId("descendantUnitId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "descendant_unit_id", nullable = false)
    private OrganizationalUnit descendantUnit;

    @Column(nullable = false)
    private int depth;

    public OrganizationalUnitClosure() {}

    public OrganizationalUnitClosure(
            Tenant tenant,
            OrganizationalUnit ancestorUnit,
            OrganizationalUnit descendantUnit,
            int depth) {
        this.tenant = tenant;
        this.ancestorUnit = ancestorUnit;
        this.descendantUnit = descendantUnit;
        this.depth = depth;

        this.id =
                new OrganizationalUnitClosureId(
                        tenant.getId(), ancestorUnit.getId(), descendantUnit.getId());
    }

    public OrganizationalUnitClosureId getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public OrganizationalUnit getAncestorUnit() {
        return ancestorUnit;
    }

    public OrganizationalUnit getDescendantUnit() {
        return descendantUnit;
    }

    public int getDepth() {
        return depth;
    }

    public void setId(OrganizationalUnitClosureId id) {
        this.id = id;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setAncestorUnit(OrganizationalUnit ancestorUnit) {
        this.ancestorUnit = ancestorUnit;
    }

    public void setDescendantUnit(OrganizationalUnit descendantUnit) {
        this.descendantUnit = descendantUnit;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
