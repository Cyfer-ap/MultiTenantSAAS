package com.chacha.multitenantsaas.externalaccess;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExternalAccessGrantCapabilityRepository
        extends JpaRepository<ExternalAccessGrantCapability, UUID> {

    List<ExternalAccessGrantCapability> findByTenantIdAndProjectIdAndGrantIdOrderByCapabilityAsc(
            UUID tenantId, UUID projectId, UUID grantId);
}
