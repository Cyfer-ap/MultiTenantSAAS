package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class OrganizationalUnitClosureId
        implements Serializable {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "ancestor_unit_id")
    private UUID ancestorUnitId;

    @Column(name = "descendant_unit_id")
    private UUID descendantUnitId;

    public OrganizationalUnitClosureId() {
    }

    public OrganizationalUnitClosureId(
            UUID tenantId,
            UUID ancestorUnitId,
            UUID descendantUnitId
    ) {
        this.tenantId = tenantId;
        this.ancestorUnitId = ancestorUnitId;
        this.descendantUnitId = descendantUnitId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getAncestorUnitId() {
        return ancestorUnitId;
    }

    public UUID getDescendantUnitId() {
        return descendantUnitId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public void setAncestorUnitId(
            UUID ancestorUnitId
    ) {
        this.ancestorUnitId = ancestorUnitId;
    }

    public void setDescendantUnitId(
            UUID descendantUnitId
    ) {
        this.descendantUnitId =
                descendantUnitId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object
                instanceof OrganizationalUnitClosureId that)) {
            return false;
        }

        return Objects.equals(
                tenantId,
                that.tenantId
        ) && Objects.equals(
                ancestorUnitId,
                that.ancestorUnitId
        ) && Objects.equals(
                descendantUnitId,
                that.descendantUnitId
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tenantId,
                ancestorUnitId,
                descendantUnitId
        );
    }
}