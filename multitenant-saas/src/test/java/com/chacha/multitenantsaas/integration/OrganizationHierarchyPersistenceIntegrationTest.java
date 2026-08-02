package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitClosure;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.OrganizationalUnitClosureRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationHierarchyPersistenceIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrganizationalUnitRepository
            organizationalUnitRepository;

    @Autowired
    private OrganizationalUnitClosureRepository
            organizationalUnitClosureRepository;

    @Test
    void storesAndTraversesTenantScopedHierarchy() {
        Tenant tenant = createTenant(
                "hierarchy"
        );

        OrganizationalUnit engineering =
                organizationalUnitRepository.saveAndFlush(
                        new OrganizationalUnit(
                                tenant,
                                null,
                                "Engineering",
                                "ENG",
                                OrganizationalUnitType.DIVISION
                        )
                );

        OrganizationalUnit platform =
                organizationalUnitRepository.saveAndFlush(
                        new OrganizationalUnit(
                                tenant,
                                engineering,
                                "Platform",
                                "PLATFORM",
                                OrganizationalUnitType.DEPARTMENT
                        )
                );

        OrganizationalUnit backend =
                organizationalUnitRepository.saveAndFlush(
                        new OrganizationalUnit(
                                tenant,
                                platform,
                                "Backend",
                                "BACKEND",
                                OrganizationalUnitType.TEAM
                        )
                );

        organizationalUnitClosureRepository.saveAllAndFlush(
                List.of(
                        new OrganizationalUnitClosure(
                                tenant,
                                engineering,
                                engineering,
                                0
                        ),
                        new OrganizationalUnitClosure(
                                tenant,
                                platform,
                                platform,
                                0
                        ),
                        new OrganizationalUnitClosure(
                                tenant,
                                backend,
                                backend,
                                0
                        ),
                        new OrganizationalUnitClosure(
                                tenant,
                                engineering,
                                platform,
                                1
                        ),
                        new OrganizationalUnitClosure(
                                tenant,
                                platform,
                                backend,
                                1
                        ),
                        new OrganizationalUnitClosure(
                                tenant,
                                engineering,
                                backend,
                                2
                        )
                )
        );

        List<OrganizationalUnit> rootUnits =
                organizationalUnitRepository
                        .findAllByTenant_IdAndParentUnitIsNullOrderByNameAsc(
                                tenant.getId()
                        );

        assertEquals(1, rootUnits.size());
        assertEquals(
                engineering.getId(),
                rootUnits.getFirst().getId()
        );

        List<OrganizationalUnit> engineeringChildren =
                organizationalUnitRepository
                        .findAllByTenant_IdAndParentUnit_IdOrderByNameAsc(
                                tenant.getId(),
                                engineering.getId()
                        );

        assertEquals(1, engineeringChildren.size());
        assertEquals(
                platform.getId(),
                engineeringChildren.getFirst().getId()
        );

        List<OrganizationalUnitClosure> descendants =
                organizationalUnitClosureRepository
                        .findDescendantPaths(
                                tenant.getId(),
                                engineering.getId()
                        );

        assertEquals(3, descendants.size());

        assertEquals(
                List.of(0, 1, 2),
                descendants.stream()
                        .map(
                                OrganizationalUnitClosure::getDepth
                        )
                        .toList()
        );

        assertEquals(
                List.of(
                        engineering.getId(),
                        platform.getId(),
                        backend.getId()
                ),
                descendants.stream()
                        .map(
                                path ->
                                        path.getDescendantUnit()
                                                .getId()
                        )
                        .toList()
        );

        List<OrganizationalUnitClosure> ancestors =
                organizationalUnitClosureRepository
                        .findAncestorPaths(
                                tenant.getId(),
                                backend.getId()
                        );

        assertEquals(3, ancestors.size());

        assertEquals(
                List.of(
                        backend.getId(),
                        platform.getId(),
                        engineering.getId()
                ),
                ancestors.stream()
                        .map(
                                path ->
                                        path.getAncestorUnit()
                                                .getId()
                        )
                        .toList()
        );

        assertTrue(
                organizationalUnitClosureRepository
                        .existsByTenant_IdAndAncestorUnit_IdAndDescendantUnit_Id(
                                tenant.getId(),
                                engineering.getId(),
                                backend.getId()
                        )
        );

        assertFalse(
                organizationalUnitClosureRepository
                        .existsByTenant_IdAndAncestorUnit_IdAndDescendantUnit_Id(
                                tenant.getId(),
                                backend.getId(),
                                engineering.getId()
                        )
        );

        Tenant otherTenant = createTenant(
                "other-hierarchy"
        );

        assertTrue(
                organizationalUnitRepository
                        .findByTenant_IdAndId(
                                otherTenant.getId(),
                                backend.getId()
                        )
                        .isEmpty()
        );

        assertFalse(
                organizationalUnitClosureRepository
                        .existsByTenant_IdAndAncestorUnit_IdAndDescendantUnit_Id(
                                otherTenant.getId(),
                                engineering.getId(),
                                backend.getId()
                        )
        );
    }

    @Test
    void tenantScopedLookupDoesNotExposeAnotherTenantsUnit() {
        Tenant firstTenant = createTenant(
                "first-lookup"
        );

        Tenant secondTenant = createTenant(
                "second-lookup"
        );

        OrganizationalUnit secondTenantUnit =
                organizationalUnitRepository.saveAndFlush(
                        new OrganizationalUnit(
                                secondTenant,
                                null,
                                "Second Tenant Unit",
                                "SECOND-TENANT-UNIT",
                                OrganizationalUnitType.DIVISION
                        )
                );

        assertTrue(
                organizationalUnitRepository
                        .findByTenant_IdAndId(
                                secondTenant.getId(),
                                secondTenantUnit.getId()
                        )
                        .isPresent()
        );

        assertTrue(
                organizationalUnitRepository
                        .findByTenant_IdAndId(
                                firstTenant.getId(),
                                secondTenantUnit.getId()
                        )
                        .isEmpty()
        );

        assertTrue(
                organizationalUnitRepository
                        .findAllByTenant_IdOrderByNameAsc(
                                firstTenant.getId()
                        )
                        .isEmpty()
        );

        assertEquals(
                1,
                organizationalUnitRepository
                        .findAllByTenant_IdOrderByNameAsc(
                                secondTenant.getId()
                        )
                        .size()
        );
    }

    private Tenant createTenant(String prefix) {
        String suffix = UUID.randomUUID()
                .toString()
                .substring(0, 8);

        Tenant tenant = new Tenant(
                prefix + " Tenant",
                prefix + "-" + suffix
        );

        return tenantRepository.saveAndFlush(tenant);
    }
}