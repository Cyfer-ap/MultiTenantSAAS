package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OrganizationalUnitClosure;
import com.chacha.multitenantsaas.entity.OrganizationalUnitClosureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationalUnitClosureRepository
        extends JpaRepository<
        OrganizationalUnitClosure,
        OrganizationalUnitClosureId
        > {

    boolean
    existsByTenant_IdAndAncestorUnit_IdAndDescendantUnit_Id(
            UUID tenantId,
            UUID ancestorUnitId,
            UUID descendantUnitId
    );

    Optional<OrganizationalUnitClosure>
    findByTenant_IdAndAncestorUnit_IdAndDescendantUnit_Id(
            UUID tenantId,
            UUID ancestorUnitId,
            UUID descendantUnitId
    );

    @Query("""
            SELECT closure
            FROM OrganizationalUnitClosure closure
            JOIN FETCH closure.descendantUnit descendant
            WHERE closure.tenant.id = :tenantId
              AND closure.ancestorUnit.id = :ancestorUnitId
            ORDER BY
                closure.depth ASC,
                descendant.name ASC
            """)
    List<OrganizationalUnitClosure> findDescendantPaths(
            @Param("tenantId") UUID tenantId,
            @Param("ancestorUnitId") UUID ancestorUnitId
    );

    @Query("""
            SELECT closure
            FROM OrganizationalUnitClosure closure
            JOIN FETCH closure.ancestorUnit ancestor
            WHERE closure.tenant.id = :tenantId
              AND closure.descendantUnit.id = :descendantUnitId
            ORDER BY
                closure.depth ASC,
                ancestor.name ASC
            """)
    List<OrganizationalUnitClosure> findAncestorPaths(
            @Param("tenantId") UUID tenantId,
            @Param("descendantUnitId") UUID descendantUnitId
    );
}