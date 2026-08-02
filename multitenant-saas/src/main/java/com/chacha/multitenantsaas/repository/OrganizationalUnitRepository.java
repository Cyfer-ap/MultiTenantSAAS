package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationalUnitRepository
        extends JpaRepository<OrganizationalUnit, UUID> {

    Optional<OrganizationalUnit> findByTenant_IdAndId(
            UUID tenantId,
            UUID organizationalUnitId
    );

    boolean existsByTenant_IdAndCodeIgnoreCase(
            UUID tenantId,
            String code
    );

    boolean existsByTenant_IdAndCodeIgnoreCaseAndIdNot(
            UUID tenantId,
            String code,
            UUID excludedOrganizationalUnitId
    );

    List<OrganizationalUnit>
    findAllByTenant_IdAndParentUnitIsNullOrderByNameAsc(
            UUID tenantId
    );

    List<OrganizationalUnit>
    findAllByTenant_IdAndParentUnit_IdOrderByNameAsc(
            UUID tenantId,
            UUID parentUnitId
    );

    List<OrganizationalUnit>
    findAllByTenant_IdOrderByNameAsc(
            UUID tenantId
    );
}