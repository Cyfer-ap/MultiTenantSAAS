package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chacha.multitenantsaas.dto.OrganizationalUnitMoveRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitPathResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitUpdateRequest;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationHierarchyMutationIntegrationTest {

    @Autowired private TenantRepository tenantRepository;

    @Autowired private OrganizationHierarchyService organizationHierarchyService;

    @Test
    void updatesUnitDetailsAndStatus() {
        Tenant tenant = createTenant("update-unit");

        OrganizationalUnit unit =
                createUnit(
                        tenant,
                        null,
                        "Engineering",
                        "ENGINEERING",
                        OrganizationalUnitType.DIVISION);

        OrganizationalUnitResponse updated =
                organizationHierarchyService.updateUnit(
                        tenant.getId(),
                        unit.getId(),
                        new OrganizationalUnitUpdateRequest(
                                "  Product Engineering  ",
                                " product_eng ",
                                OrganizationalUnitType.DEPARTMENT));

        assertEquals("Product Engineering", updated.name());

        assertEquals("PRODUCT_ENG", updated.code());

        assertEquals(OrganizationalUnitType.DEPARTMENT, updated.type());

        assertEquals(OrganizationalUnitStatus.ACTIVE, updated.status());

        OrganizationalUnitResponse inactive =
                organizationHierarchyService.updateUnitStatus(
                        tenant.getId(),
                        unit.getId(),
                        new OrganizationalUnitStatusUpdateRequest(
                                OrganizationalUnitStatus.INACTIVE));

        assertEquals(OrganizationalUnitStatus.INACTIVE, inactive.status());

        OrganizationalUnitResponse activeAgain =
                organizationHierarchyService.updateUnitStatus(
                        tenant.getId(),
                        unit.getId(),
                        new OrganizationalUnitStatusUpdateRequest(OrganizationalUnitStatus.ACTIVE));

        assertEquals(OrganizationalUnitStatus.ACTIVE, activeAgain.status());
    }

    @Test
    void rejectsDuplicateCodeDuringUpdate() {
        Tenant tenant = createTenant("update-duplicate");

        createUnit(tenant, null, "Engineering", "ENGINEERING", OrganizationalUnitType.DIVISION);

        OrganizationalUnit operations =
                createUnit(
                        tenant,
                        null,
                        "Operations",
                        "OPERATIONS",
                        OrganizationalUnitType.DEPARTMENT);

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        organizationHierarchyService.updateUnit(
                                tenant.getId(),
                                operations.getId(),
                                new OrganizationalUnitUpdateRequest(
                                        "Operations Updated",
                                        " engineering ",
                                        OrganizationalUnitType.DEPARTMENT)));
    }

    @Test
    void movesEntireSubtreeAndRebuildsPaths() {
        Tenant tenant = createTenant("move-subtree");

        OrganizationalUnit firstCompany =
                createUnit(
                        tenant,
                        null,
                        "First Company",
                        "FIRST-COMPANY",
                        OrganizationalUnitType.COMPANY);

        OrganizationalUnit engineering =
                createUnit(
                        tenant,
                        firstCompany.getId(),
                        "Engineering",
                        "ENGINEERING",
                        OrganizationalUnitType.DIVISION);

        OrganizationalUnit backend =
                createUnit(
                        tenant,
                        engineering.getId(),
                        "Backend",
                        "BACKEND",
                        OrganizationalUnitType.TEAM);

        OrganizationalUnit apiTeam =
                createUnit(
                        tenant,
                        backend.getId(),
                        "API Team",
                        "API-TEAM",
                        OrganizationalUnitType.SUBTEAM);

        OrganizationalUnit secondCompany =
                createUnit(
                        tenant,
                        null,
                        "Second Company",
                        "SECOND-COMPANY",
                        OrganizationalUnitType.COMPANY);

        OrganizationalUnit operations =
                createUnit(
                        tenant,
                        secondCompany.getId(),
                        "Operations",
                        "OPERATIONS",
                        OrganizationalUnitType.DEPARTMENT);

        OrganizationalUnitResponse moved =
                organizationHierarchyService.moveUnit(
                        tenant.getId(),
                        engineering.getId(),
                        new OrganizationalUnitMoveRequest(operations.getId()));

        assertEquals(operations.getId(), moved.parentUnitId());

        List<OrganizationalUnitPathResponse> apiAncestors =
                organizationHierarchyService.getAncestors(tenant.getId(), apiTeam.getId());

        assertEquals(
                List.of(
                        apiTeam.getId(),
                        backend.getId(),
                        engineering.getId(),
                        operations.getId(),
                        secondCompany.getId()),
                apiAncestors.stream().map(OrganizationalUnitPathResponse::id).toList());

        assertEquals(
                List.of(0, 1, 2, 3, 4),
                apiAncestors.stream().map(OrganizationalUnitPathResponse::depth).toList());

        List<OrganizationalUnitPathResponse> firstCompanyDescendants =
                organizationHierarchyService.getDescendants(tenant.getId(), firstCompany.getId());

        assertEquals(
                List.of(firstCompany.getId()),
                firstCompanyDescendants.stream().map(OrganizationalUnitPathResponse::id).toList());

        List<OrganizationalUnitPathResponse> engineeringDescendants =
                organizationHierarchyService.getDescendants(tenant.getId(), engineering.getId());

        assertEquals(
                List.of(engineering.getId(), backend.getId(), apiTeam.getId()),
                engineeringDescendants.stream().map(OrganizationalUnitPathResponse::id).toList());

        assertEquals(
                List.of(0, 1, 2),
                engineeringDescendants.stream()
                        .map(OrganizationalUnitPathResponse::depth)
                        .toList());
    }

    @Test
    void movesNestedUnitToRootLevel() {
        Tenant tenant = createTenant("move-root");

        OrganizationalUnit company =
                createUnit(tenant, null, "Company", "COMPANY", OrganizationalUnitType.COMPANY);

        OrganizationalUnit engineering =
                createUnit(
                        tenant,
                        company.getId(),
                        "Engineering",
                        "ENGINEERING",
                        OrganizationalUnitType.DIVISION);

        OrganizationalUnit backend =
                createUnit(
                        tenant,
                        engineering.getId(),
                        "Backend",
                        "BACKEND",
                        OrganizationalUnitType.TEAM);

        OrganizationalUnitResponse moved =
                organizationHierarchyService.moveUnit(
                        tenant.getId(),
                        engineering.getId(),
                        new OrganizationalUnitMoveRequest(null));

        assertNull(moved.parentUnitId());

        assertEquals(
                List.of(engineering.getId()),
                organizationHierarchyService
                        .getAncestors(tenant.getId(), engineering.getId())
                        .stream()
                        .map(OrganizationalUnitPathResponse::id)
                        .toList());

        assertEquals(
                List.of(backend.getId(), engineering.getId()),
                organizationHierarchyService.getAncestors(tenant.getId(), backend.getId()).stream()
                        .map(OrganizationalUnitPathResponse::id)
                        .toList());

        assertEquals(
                List.of(company.getId()),
                organizationHierarchyService
                        .getDescendants(tenant.getId(), company.getId())
                        .stream()
                        .map(OrganizationalUnitPathResponse::id)
                        .toList());
    }

    @Test
    void rejectsSelfParentingAndDescendantCycle() {
        Tenant tenant = createTenant("move-cycle");

        OrganizationalUnit company =
                createUnit(tenant, null, "Company", "COMPANY", OrganizationalUnitType.COMPANY);

        OrganizationalUnit engineering =
                createUnit(
                        tenant,
                        company.getId(),
                        "Engineering",
                        "ENGINEERING",
                        OrganizationalUnitType.DIVISION);

        OrganizationalUnit backend =
                createUnit(
                        tenant,
                        engineering.getId(),
                        "Backend",
                        "BACKEND",
                        OrganizationalUnitType.TEAM);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationHierarchyService.moveUnit(
                                tenant.getId(),
                                engineering.getId(),
                                new OrganizationalUnitMoveRequest(engineering.getId())));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationHierarchyService.moveUnit(
                                tenant.getId(),
                                company.getId(),
                                new OrganizationalUnitMoveRequest(backend.getId())));
    }

    @Test
    void rejectsInactiveAndCrossTenantDestinationParents() {
        Tenant firstTenant = createTenant("move-first");

        Tenant secondTenant = createTenant("move-second");

        OrganizationalUnit movingUnit =
                createUnit(
                        firstTenant,
                        null,
                        "Moving Unit",
                        "MOVING-UNIT",
                        OrganizationalUnitType.DIVISION);

        OrganizationalUnit inactiveParent =
                createUnit(
                        firstTenant,
                        null,
                        "Inactive Parent",
                        "INACTIVE-PARENT",
                        OrganizationalUnitType.DEPARTMENT);

        organizationHierarchyService.updateUnitStatus(
                firstTenant.getId(),
                inactiveParent.getId(),
                new OrganizationalUnitStatusUpdateRequest(OrganizationalUnitStatus.INACTIVE));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationHierarchyService.moveUnit(
                                firstTenant.getId(),
                                movingUnit.getId(),
                                new OrganizationalUnitMoveRequest(inactiveParent.getId())));

        OrganizationalUnit foreignParent =
                createUnit(
                        secondTenant,
                        null,
                        "Foreign Parent",
                        "FOREIGN-PARENT",
                        OrganizationalUnitType.DIVISION);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationHierarchyService.moveUnit(
                                firstTenant.getId(),
                                movingUnit.getId(),
                                new OrganizationalUnitMoveRequest(foreignParent.getId())));
    }

    private OrganizationalUnit createUnit(
            Tenant tenant,
            UUID parentUnitId,
            String name,
            String code,
            OrganizationalUnitType type) {
        return organizationHierarchyService.createUnit(
                tenant.getId(), parentUnitId, name, code, type);
    }

    private Tenant createTenant(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Tenant tenant = new Tenant(prefix + " Tenant", prefix + "-" + suffix);

        return tenantRepository.saveAndFlush(tenant);
    }
}
