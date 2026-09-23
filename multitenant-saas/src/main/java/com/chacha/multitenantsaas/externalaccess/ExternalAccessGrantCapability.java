package com.chacha.multitenantsaas.externalaccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_access_grant_capabilities")
public class ExternalAccessGrantCapability {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "grant_id", nullable = false)
    private UUID grantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability", nullable = false, length = 40)
    private ExternalAccessCapability capability;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExternalAccessGrantCapability() {}

    public ExternalAccessGrantCapability(
            UUID tenantId, UUID projectId, UUID grantId, ExternalAccessCapability capability) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.grantId = grantId;
        this.capability = capability;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public ExternalAccessCapability getCapability() {
        return capability;
    }
}
