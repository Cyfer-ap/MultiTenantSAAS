package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chacha.multitenantsaas.dto.OrganizationalUnitPathResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitTreeResponse;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
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
class OrganizationHierarchyReadIntegrationTest {

    @Autowired private TenantRepository tenantRepository;

    @Autowired private OrganizationHierarchyService organizationHierarchyService;

    @Test
    void returnsCompleteTreeRootsChildrenAndSubtree() {
        Tenant tenant = createTenant("read-tree");

        OrganizationalUnit company =
                createUnit(tenant, null, "Company", "COMPANY", OrganizationalUnitType.COMPANY);

        OrganizationalUnit engineering =
                createUnit(
                        tenant,
                        company.getId(),
                        "Engineering",
                        "ENGINEERING",
                        OrganizationalUnitType.DIVISION);

        createUnit(
                tenant, engineering.getId(), "Frontend", "FRONTEND", OrganizationalUnitType.TEAM);

        createUnit(tenant, engineering.getId(), "Backend", "BACKEND", OrganizationalUnitType.TEAM);

        createUnit(
                tenant,
                company.getId(),
                "Operations",
                "OPERATIONS",
                OrganizationalUnitType.DEPARTMENT);

        createUnit(tenant, null, "Advisory", "ADVISORY", OrganizationalUnitType.DIVISION);

        List<OrganizationalUnitTreeResponse> tree =
                organizationHierarchyService.getTree(tenant.getId());

        assertEquals(2, tree.size());

        assertEquals(
                List.of("Advisory", "Company"),
                tree.stream().map(OrganizationalUnitTreeResponse::name).toList());

        OrganizationalUnitTreeResponse companyNode = tree.get(1);

        assertEquals(company.getId(), companyNode.id());

        assertEquals(
                List.of("Engineering", "Operations"),
                companyNode.children().stream().map(OrganizationalUnitTreeResponse::name).toList());

        OrganizationalUnitTreeResponse engineeringNode = companyNode.children().getFirst();

        assertEquals(
                List.of("Backend", "Frontend"),
                engineeringNode.children().stream()
                        .map(OrganizationalUnitTreeResponse::name)
                        .toList());

        List<OrganizationalUnitResponse> roots =
                organizationHierarchyService.getRootUnits(tenant.getId());

        assertEquals(
                List.of("Advisory", "Company"),
                roots.stream().map(OrganizationalUnitResponse::name).toList());

        List<OrganizationalUnitResponse> companyChildren =
                organizationHierarchyService.getDirectChildren(tenant.getId(), company.getId());

        assertEquals(
                List.of("Engineering", "Operations"),
                companyChildren.stream().map(OrganizationalUnitResponse::name).toList());

        OrganizationalUnitTreeResponse subtree =
                organizationHierarchyService.getSubtree(tenant.getId(), engineering.getId());

        assertEquals("Engineering", subtree.name());

        assertEquals(company.getId(), subtree.parentUnitId());

        assertEquals(
                List.of("Backend", "Frontend"),
                subtree.children().stream().map(OrganizationalUnitTreeResponse::name).toList());
    }

    @Test
    void returnsAncestorAndDescendantPathsWithDepth() {
        Tenant tenant = createTenant("read-paths");

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

        List<OrganizationalUnitPathResponse> ancestors =
                organizationHierarchyService.getAncestors(tenant.getId(), backend.getId());

        assertEquals(
                List.of(backend.getId(), engineering.getId(), company.getId()),
                ancestors.stream().map(OrganizationalUnitPathResponse::id).toList());

        assertEquals(
                List.of(0, 1, 2),
                ancestors.stream().map(OrganizationalUnitPathResponse::depth).toList());

        List<OrganizationalUnitPathResponse> descendants =
                organizationHierarchyService.getDescendants(tenant.getId(), company.getId());

        assertEquals(
                List.of(company.getId(), engineering.getId(), backend.getId()),
                descendants.stream().map(OrganizationalUnitPathResponse::id).toList());

        assertEquals(
                List.of(0, 1, 2),
                descendants.stream().map(OrganizationalUnitPathResponse::depth).toList());
    }

    @Test
    void hierarchyReadOperationsAreTenantScoped() {
        Tenant firstTenant = createTenant("first-read");

        Tenant secondTenant = createTenant("second-read");

        OrganizationalUnit secondTenantUnit =
                createUnit(
                        secondTenant,
                        null,
                        "Second Tenant Unit",
                        "SECOND-UNIT",
                        OrganizationalUnitType.DIVISION);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationHierarchyService.getUnit(
                                firstTenant.getId(), secondTenantUnit.getId()));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationHierarchyService.getSubtree(
                                firstTenant.getId(), secondTenantUnit.getId()));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationHierarchyService.getDirectChildren(
                                firstTenant.getId(), secondTenantUnit.getId()));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationHierarchyService.getAncestors(
                                firstTenant.getId(), secondTenantUnit.getId()));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationHierarchyService.getDescendants(
                                firstTenant.getId(), secondTenantUnit.getId()));
    }

    @Test
    void returnsEmptyTreeForExistingTenantWithoutUnits() {
        Tenant tenant = createTenant("empty-tree");

        assertTrue(organizationHierarchyService.getTree(tenant.getId()).isEmpty());

        assertTrue(organizationHierarchyService.getRootUnits(tenant.getId()).isEmpty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> organizationHierarchyService.getTree(UUID.randomUUID()));
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
