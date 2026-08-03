package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationAssignmentPersistenceIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserOrganizationAssignmentRepository
            userOrganizationAssignmentRepository;

    @Autowired
    private OrganizationHierarchyService
            organizationHierarchyService;

    @Test
    void storesPrimarySecondaryAndReportingAssignments() {
        Tenant tenant = createTenant(
                "assignment-persistence"
        );

        AppUser administrator = createUser(
                tenant,
                "Tenant Administrator",
                UserRole.TENANT_ADMIN
        );

        AppUser manager = createUser(
                tenant,
                "Engineering Manager",
                UserRole.TENANT_MANAGER
        );

        AppUser member = createUser(
                tenant,
                "Backend Engineer",
                UserRole.TENANT_USER
        );

        OrganizationalUnit company = createUnit(
                tenant,
                null,
                "Company",
                "COMPANY",
                OrganizationalUnitType.COMPANY
        );

        OrganizationalUnit engineering = createUnit(
                tenant,
                company.getId(),
                "Engineering",
                "ENGINEERING",
                OrganizationalUnitType.DIVISION
        );

        OrganizationalUnit backend = createUnit(
                tenant,
                engineering.getId(),
                "Backend",
                "BACKEND",
                OrganizationalUnitType.TEAM
        );

        Instant validFrom =
                Instant.now()
                        .minus(
                                1,
                                ChronoUnit.DAYS
                        );

        UserOrganizationAssignment
                managerAssignment =
                new UserOrganizationAssignment(
                        tenant,
                        manager,
                        engineering,
                        null,
                        "Engineering Manager",
                        true,
                        validFrom,
                        null,
                        administrator
                );

        managerAssignment =
                userOrganizationAssignmentRepository
                        .saveAndFlush(
                                managerAssignment
                        );

        UserOrganizationAssignment
                memberPrimaryAssignment =
                new UserOrganizationAssignment(
                        tenant,
                        member,
                        backend,
                        managerAssignment,
                        "Backend Engineer",
                        true,
                        validFrom,
                        null,
                        administrator
                );

        UserOrganizationAssignment
                memberSecondaryAssignment =
                new UserOrganizationAssignment(
                        tenant,
                        member,
                        company,
                        null,
                        "Architecture Contributor",
                        false,
                        validFrom,
                        null,
                        administrator
                );

        userOrganizationAssignmentRepository
                .saveAllAndFlush(
                        List.of(
                                memberPrimaryAssignment,
                                memberSecondaryAssignment
                        )
                );

        List<UserOrganizationAssignment>
                memberAssignments =
                userOrganizationAssignmentRepository
                        .findUserAssignments(
                                tenant.getId(),
                                member.getId()
                        );

        assertEquals(
                2,
                memberAssignments.size()
        );

        assertTrue(
                memberAssignments
                        .getFirst()
                        .isPrimaryAssignment()
        );

        assertEquals(
                backend.getId(),
                memberAssignments
                        .getFirst()
                        .getOrganizationalUnit()
                        .getId()
        );

        assertEquals(
                managerAssignment.getId(),
                memberAssignments
                        .getFirst()
                        .getReportsToAssignment()
                        .getId()
        );

        List<UserOrganizationAssignment>
                backendAssignments =
                userOrganizationAssignmentRepository
                        .findUnitAssignments(
                                tenant.getId(),
                                backend.getId()
                        );

        assertEquals(
                1,
                backendAssignments.size()
        );

        assertEquals(
                member.getId(),
                backendAssignments
                        .getFirst()
                        .getUser()
                        .getId()
        );

        List<UserOrganizationAssignment>
                directReports =
                userOrganizationAssignmentRepository
                        .findDirectReports(
                                tenant.getId(),
                                managerAssignment.getId()
                        );

        assertEquals(
                1,
                directReports.size()
        );

        assertEquals(
                member.getId(),
                directReports
                        .getFirst()
                        .getUser()
                        .getId()
        );

        assertTrue(
                userOrganizationAssignmentRepository
                        .existsByTenant_IdAndUser_IdAndPrimaryAssignmentTrueAndStatus(
                                tenant.getId(),
                                member.getId(),
                                OrganizationAssignmentStatus.ACTIVE
                        )
        );
    }

    @Test
    void effectiveQueryRespectsStatusAndValidity() {
        Tenant tenant = createTenant(
                "assignment-validity"
        );

        AppUser administrator = createUser(
                tenant,
                "Assignment Administrator",
                UserRole.TENANT_ADMIN
        );

        AppUser member = createUser(
                tenant,
                "Temporal Member",
                UserRole.TENANT_USER
        );

        OrganizationalUnit activeUnit = createUnit(
                tenant,
                null,
                "Active Unit",
                "ACTIVE-UNIT",
                OrganizationalUnitType.DEPARTMENT
        );

        OrganizationalUnit futureUnit = createUnit(
                tenant,
                null,
                "Future Unit",
                "FUTURE-UNIT",
                OrganizationalUnitType.DEPARTMENT
        );

        OrganizationalUnit expiredUnit = createUnit(
                tenant,
                null,
                "Expired Unit",
                "EXPIRED-UNIT",
                OrganizationalUnitType.DEPARTMENT
        );

        OrganizationalUnit inactiveUnit = createUnit(
                tenant,
                null,
                "Inactive Assignment Unit",
                "INACTIVE-ASSIGNMENT-UNIT",
                OrganizationalUnitType.DEPARTMENT
        );

        Instant now = Instant.now();

        UserOrganizationAssignment currentAssignment =
                new UserOrganizationAssignment(
                        tenant,
                        member,
                        activeUnit,
                        null,
                        "Current Position",
                        true,
                        now.minus(
                                1,
                                ChronoUnit.DAYS
                        ),
                        null,
                        administrator
                );

        UserOrganizationAssignment futureAssignment =
                new UserOrganizationAssignment(
                        tenant,
                        member,
                        futureUnit,
                        null,
                        "Future Position",
                        false,
                        now.plus(
                                1,
                                ChronoUnit.DAYS
                        ),
                        null,
                        administrator
                );

        UserOrganizationAssignment expiredAssignment =
                new UserOrganizationAssignment(
                        tenant,
                        member,
                        expiredUnit,
                        null,
                        "Expired Position",
                        false,
                        now.minus(
                                10,
                                ChronoUnit.DAYS
                        ),
                        now.minus(
                                1,
                                ChronoUnit.DAYS
                        ),
                        administrator
                );

        UserOrganizationAssignment inactiveAssignment =
                new UserOrganizationAssignment(
                        tenant,
                        member,
                        inactiveUnit,
                        null,
                        "Inactive Position",
                        false,
                        now.minus(
                                1,
                                ChronoUnit.DAYS
                        ),
                        null,
                        administrator
                );

        inactiveAssignment.setStatus(
                OrganizationAssignmentStatus.INACTIVE
        );

        userOrganizationAssignmentRepository
                .saveAllAndFlush(
                        List.of(
                                currentAssignment,
                                futureAssignment,
                                expiredAssignment,
                                inactiveAssignment
                        )
                );

        List<UserOrganizationAssignment>
                effectiveAssignments =
                userOrganizationAssignmentRepository
                        .findEffectiveAssignmentsForUser(
                                tenant.getId(),
                                member.getId(),
                                OrganizationAssignmentStatus.ACTIVE,
                                now
                        );

        assertEquals(
                1,
                effectiveAssignments.size()
        );

        assertEquals(
                activeUnit.getId(),
                effectiveAssignments
                        .getFirst()
                        .getOrganizationalUnit()
                        .getId()
        );

        assertTrue(
                effectiveAssignments
                        .getFirst()
                        .isPrimaryAssignment()
        );
    }

    @Test
    void assignmentLookupsAreTenantScoped() {
        Tenant firstTenant = createTenant(
                "assignment-first"
        );

        Tenant secondTenant = createTenant(
                "assignment-second"
        );

        AppUser firstAdministrator = createUser(
                firstTenant,
                "First Administrator",
                UserRole.TENANT_ADMIN
        );

        AppUser secondAdministrator = createUser(
                secondTenant,
                "Second Administrator",
                UserRole.TENANT_ADMIN
        );

        AppUser secondMember = createUser(
                secondTenant,
                "Second Tenant Member",
                UserRole.TENANT_USER
        );

        OrganizationalUnit secondUnit = createUnit(
                secondTenant,
                null,
                "Second Tenant Unit",
                "SECOND-TENANT-UNIT",
                OrganizationalUnitType.DEPARTMENT
        );

        UserOrganizationAssignment assignment =
                new UserOrganizationAssignment(
                        secondTenant,
                        secondMember,
                        secondUnit,
                        null,
                        "Second Tenant Position",
                        true,
                        Instant.now(),
                        null,
                        secondAdministrator
                );

        assignment =
                userOrganizationAssignmentRepository
                        .saveAndFlush(assignment);

        assertTrue(
                userOrganizationAssignmentRepository
                        .findByTenant_IdAndId(
                                secondTenant.getId(),
                                assignment.getId()
                        )
                        .isPresent()
        );

        assertTrue(
                userOrganizationAssignmentRepository
                        .findByTenant_IdAndId(
                                firstTenant.getId(),
                                assignment.getId()
                        )
                        .isEmpty()
        );

        assertTrue(
                userOrganizationAssignmentRepository
                        .findUserAssignments(
                                firstTenant.getId(),
                                secondMember.getId()
                        )
                        .isEmpty()
        );

        assertFalse(
                userOrganizationAssignmentRepository
                        .existsByTenant_IdAndUser_IdAndPrimaryAssignmentTrueAndStatus(
                                firstTenant.getId(),
                                secondMember.getId(),
                                OrganizationAssignmentStatus.ACTIVE
                        )
        );

        /*
         * Prevent an unused-variable warning while keeping
         * the first tenant's actor fixture explicit.
         */
        assertEquals(
                firstTenant.getId(),
                firstAdministrator
                        .getTenant()
                        .getId()
        );
    }

    private OrganizationalUnit createUnit(
            Tenant tenant,
            UUID parentUnitId,
            String name,
            String code,
            OrganizationalUnitType type
    ) {
        return organizationHierarchyService
                .createUnit(
                        tenant.getId(),
                        parentUnitId,
                        name,
                        code,
                        type
                );
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName,
            UserRole role
    ) {
        String suffix = uniqueSuffix();

        AppUser user = new AppUser(
                tenant,
                fullName,
                role.name()
                        .toLowerCase()
                        .replace('_', '.')
                        + "."
                        + suffix
                        + "@example.test",
                "test-password-hash",
                role
        );

        return appUserRepository
                .saveAndFlush(user);
    }

    private Tenant createTenant(String prefix) {
        String suffix = uniqueSuffix();

        Tenant tenant = new Tenant(
                prefix + " Tenant",
                prefix + "-" + suffix
        );

        return tenantRepository
                .saveAndFlush(tenant);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}