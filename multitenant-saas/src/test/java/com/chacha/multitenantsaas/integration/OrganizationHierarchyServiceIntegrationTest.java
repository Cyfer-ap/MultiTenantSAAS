package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitClosure;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.OrganizationalUnitClosureRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
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
class OrganizationHierarchyServiceIntegrationTest {

    @Autowired private TenantRepository tenantRepository;

    @Autowired private OrganizationalUnitRepository organizationalUnitRepository;

    @Autowired private OrganizationalUnitClosureRepository organizationalUnitClosureRepository;

    @Autowired private OrganizationHierarchyService organizationHierarchyService;

    @Test
    void createsNestedHierarchyAndGeneratesClosureRows() {
        Tenant tenant = createTenant("service-hierarchy");

        OrganizationalUnit engineering =
                organizationHierarchyService.createUnit(
                        tenant.getId(),
                        null,
                        "  Engineering  ",
                        " eng ",
                        OrganizationalUnitType.DIVISION);

        OrganizationalUnit platform =
                organizationHierarchyService.createUnit(
                        tenant.getId(),
                        engineering.getId(),
                        "Platform",
                        "platform",
                        OrganizationalUnitType.DEPARTMENT);

        OrganizationalUnit backend =
                organizationHierarchyService.createUnit(
                        tenant.getId(),
                        platform.getId(),
                        "Backend",
                        "backend",
                        OrganizationalUnitType.TEAM);

        assertNull(engineering.getParentUnit());

        assertEquals(engineering.getId(), platform.getParentUnit().getId());

        assertEquals(platform.getId(), backend.getParentUnit().getId());

        assertEquals("Engineering", engineering.getName());

        assertEquals("ENG", engineering.getCode());

        assertEquals("PLATFORM", platform.getCode());

        assertEquals("BACKEND", backend.getCode());

        List<OrganizationalUnitClosure> descendants =
                organizationalUnitClosureRepository.findDescendantPaths(
                        tenant.getId(), engineering.getId());

        assertEquals(3, descendants.size());

        assertEquals(
                List.of(0, 1, 2),
                descendants.stream().map(OrganizationalUnitClosure::getDepth).toList());

        assertEquals(
                List.of(engineering.getId(), platform.getId(), backend.getId()),
                descendants.stream().map(path -> path.getDescendantUnit().getId()).toList());

        List<OrganizationalUnitClosure> ancestors =
                organizationalUnitClosureRepository.findAncestorPaths(
                        tenant.getId(), backend.getId());

        assertEquals(3, ancestors.size());

        assertEquals(
                List.of(backend.getId(), platform.getId(), engineering.getId()),
                ancestors.stream().map(path -> path.getAncestorUnit().getId()).toList());
    }

    @Test
    void rejectsDuplicateCodeIgnoringCaseAndWhitespace() {
        Tenant tenant = createTenant("duplicate-code");

        organizationHierarchyService.createUnit(
                tenant.getId(),
                null,
                "Engineering",
                " engineering ",
                OrganizationalUnitType.DIVISION);

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        organizationHierarchyService.createUnit(
                                tenant.getId(),
                                null,
                                "Engineering Two",
                                "ENGINEERING",
                                OrganizationalUnitType.DEPARTMENT));

        assertEquals(
                1,
                organizationalUnitRepository
                        .findAllByTenant_IdOrderByNameAsc(tenant.getId())
                        .size());
    }

    @Test
    void rejectsParentFromAnotherTenantBeforeSaving() {
        Tenant firstTenant = createTenant("first-service");

        Tenant secondTenant = createTenant("second-service");

        OrganizationalUnit secondTenantParent =
                organizationHierarchyService.createUnit(
                        secondTenant.getId(),
                        null,
                        "Foreign Parent",
                        "FOREIGN-PARENT",
                        OrganizationalUnitType.DIVISION);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationHierarchyService.createUnit(
                                firstTenant.getId(),
                                secondTenantParent.getId(),
                                "Invalid Child",
                                "INVALID-CHILD",
                                OrganizationalUnitType.TEAM));

        assertTrue(
                organizationalUnitRepository
                        .findAllByTenant_IdOrderByNameAsc(firstTenant.getId())
                        .isEmpty());
    }

    @Test
    void rejectsChildCreationUnderInactiveParent() {
        Tenant tenant = createTenant("inactive-parent");

        OrganizationalUnit parent =
                organizationHierarchyService.createUnit(
                        tenant.getId(),
                        null,
                        "Inactive Parent",
                        "INACTIVE-PARENT",
                        OrganizationalUnitType.DIVISION);

        parent.setStatus(OrganizationalUnitStatus.INACTIVE);

        organizationalUnitRepository.saveAndFlush(parent);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationHierarchyService.createUnit(
                                tenant.getId(),
                                parent.getId(),
                                "Invalid Child",
                                "INVALID-CHILD",
                                OrganizationalUnitType.TEAM));

        assertEquals(
                1,
                organizationalUnitRepository
                        .findAllByTenant_IdOrderByNameAsc(tenant.getId())
                        .size());
    }

    private Tenant createTenant(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Tenant tenant = new Tenant(prefix + " Tenant", prefix + "-" + suffix);

        return tenantRepository.saveAndFlush(tenant);
    }
}
