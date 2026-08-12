package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chacha.multitenantsaas.dto.OrganizationAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.OrganizationAssignmentService;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class OrganizationAssignmentServiceIntegrationTest {

    @Autowired private OrganizationAssignmentService organizationAssignmentService;

    @Autowired private OrganizationHierarchyService organizationHierarchyService;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Test
    void createsAndReadsPrimarySecondaryAndReportingAssignments() {
        Tenant tenant = createTenant("assignment-service");

        AppUser administrator = createUser(tenant, "Tenant Administrator", UserRole.TENANT_ADMIN);

        AppUser manager = createUser(tenant, "Engineering Manager", UserRole.TENANT_MANAGER);

        AppUser engineer = createUser(tenant, "Backend Engineer", UserRole.TENANT_USER);

        OrganizationalUnit engineering =
                createUnit(
                        tenant,
                        null,
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

        Instant validFrom = Instant.now().minus(1, ChronoUnit.DAYS);

        OrganizationAssignmentResponse managerAssignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                manager.getId(),
                                engineering.getId(),
                                null,
                                "Engineering Manager",
                                true,
                                validFrom,
                                null));

        OrganizationAssignmentResponse engineerPrimaryAssignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                engineer.getId(),
                                backend.getId(),
                                managerAssignment.id(),
                                "Backend Engineer",
                                true,
                                validFrom,
                                null));

        OrganizationAssignmentResponse engineerSecondaryAssignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                engineer.getId(),
                                engineering.getId(),
                                null,
                                "Architecture Contributor",
                                false,
                                validFrom,
                                null));

        assertTrue(engineerPrimaryAssignment.primaryAssignment());

        assertFalse(engineerSecondaryAssignment.primaryAssignment());

        assertEquals(managerAssignment.id(), engineerPrimaryAssignment.reportsToAssignmentId());

        assertEquals(manager.getId(), engineerPrimaryAssignment.managerUserId());

        List<OrganizationAssignmentResponse> userAssignments =
                organizationAssignmentService.getUserAssignments(tenant.getId(), engineer.getId());

        assertEquals(2, userAssignments.size());

        assertTrue(userAssignments.getFirst().primaryAssignment());

        List<OrganizationAssignmentResponse> backendAssignments =
                organizationAssignmentService.getUnitAssignments(tenant.getId(), backend.getId());

        assertEquals(1, backendAssignments.size());

        assertEquals(engineer.getId(), backendAssignments.getFirst().userId());

        List<OrganizationAssignmentResponse> directReports =
                organizationAssignmentService.getDirectReports(
                        tenant.getId(), managerAssignment.id());

        assertEquals(1, directReports.size());

        assertEquals(engineer.getId(), directReports.getFirst().userId());
    }

    @Test
    void rejectsOverlappingPrimaryAndAllowsSequentialPrimary() {
        Tenant tenant = createTenant("assignment-primary");

        AppUser administrator = createUser(tenant, "Primary Administrator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Primary Member", UserRole.TENANT_USER);

        OrganizationalUnit firstUnit =
                createUnit(
                        tenant,
                        null,
                        "First Unit",
                        "FIRST-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        OrganizationalUnit secondUnit =
                createUnit(
                        tenant,
                        null,
                        "Second Unit",
                        "SECOND-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        Instant firstStart = Instant.now().minus(1, ChronoUnit.DAYS);

        Instant firstEnd = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        organizationAssignmentService.createAssignment(
                tenant.getId(),
                administrator.getId(),
                request(
                        member.getId(),
                        firstUnit.getId(),
                        null,
                        "First Position",
                        true,
                        firstStart,
                        firstEnd));

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                request(
                                        member.getId(),
                                        secondUnit.getId(),
                                        null,
                                        "Overlapping Position",
                                        true,
                                        Instant.now(),
                                        firstEnd.plus(2, ChronoUnit.DAYS))));

        OrganizationAssignmentResponse sequentialAssignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                member.getId(),
                                secondUnit.getId(),
                                null,
                                "Sequential Position",
                                true,
                                firstEnd,
                                null));

        assertNotNull(sequentialAssignment.id());

        assertEquals(firstEnd, sequentialAssignment.validFrom());
    }

    @Test
    void rejectsCrossTenantAssignmentReferences() {
        Tenant firstTenant = createTenant("assignment-first");

        Tenant secondTenant = createTenant("assignment-second");

        AppUser firstAdministrator =
                createUser(firstTenant, "First Administrator", UserRole.TENANT_ADMIN);

        AppUser firstMember = createUser(firstTenant, "First Member", UserRole.TENANT_USER);

        AppUser secondAdministrator =
                createUser(secondTenant, "Second Administrator", UserRole.TENANT_ADMIN);

        AppUser secondMember = createUser(secondTenant, "Second Member", UserRole.TENANT_USER);

        OrganizationalUnit firstUnit =
                createUnit(
                        firstTenant,
                        null,
                        "First Tenant Unit",
                        "FIRST-TENANT-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        OrganizationalUnit secondUnit =
                createUnit(
                        secondTenant,
                        null,
                        "Second Tenant Unit",
                        "SECOND-TENANT-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        Instant validFrom = Instant.now();

        OrganizationAssignmentResponse secondManagerAssignment =
                organizationAssignmentService.createAssignment(
                        secondTenant.getId(),
                        secondAdministrator.getId(),
                        request(
                                secondAdministrator.getId(),
                                secondUnit.getId(),
                                null,
                                "Second Tenant Manager",
                                true,
                                validFrom,
                                null));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                firstTenant.getId(),
                                firstAdministrator.getId(),
                                request(
                                        secondMember.getId(),
                                        firstUnit.getId(),
                                        null,
                                        "Cross Tenant User",
                                        false,
                                        validFrom,
                                        null)));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                firstTenant.getId(),
                                firstAdministrator.getId(),
                                request(
                                        firstMember.getId(),
                                        secondUnit.getId(),
                                        null,
                                        "Cross Tenant Unit",
                                        false,
                                        validFrom,
                                        null)));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                firstTenant.getId(),
                                secondAdministrator.getId(),
                                request(
                                        firstMember.getId(),
                                        firstUnit.getId(),
                                        null,
                                        "Cross Tenant Creator",
                                        false,
                                        validFrom,
                                        null)));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                firstTenant.getId(),
                                firstAdministrator.getId(),
                                request(
                                        firstMember.getId(),
                                        firstUnit.getId(),
                                        secondManagerAssignment.id(),
                                        "Cross Tenant Manager",
                                        false,
                                        validFrom,
                                        null)));
    }

    @Test
    void rejectsInactiveUsersAndOrganizationalUnits() {
        Tenant tenant = createTenant("assignment-inactive");

        AppUser administrator =
                createUser(tenant, "Inactive Test Administrator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Inactive Test Member", UserRole.TENANT_USER);

        OrganizationalUnit unit =
                createUnit(
                        tenant,
                        null,
                        "Inactive Test Unit",
                        "INACTIVE-TEST-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        member.setStatus(UserStatus.INACTIVE);

        appUserRepository.saveAndFlush(member);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                request(
                                        member.getId(),
                                        unit.getId(),
                                        null,
                                        "Inactive User Position",
                                        false,
                                        Instant.now(),
                                        null)));

        member.setStatus(UserStatus.ACTIVE);

        appUserRepository.saveAndFlush(member);

        unit.setStatus(OrganizationalUnitStatus.INACTIVE);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                request(
                                        member.getId(),
                                        unit.getId(),
                                        null,
                                        "Inactive Unit Position",
                                        false,
                                        Instant.now(),
                                        null)));
    }

    @Test
    void rejectsInvalidValidityAndInsufficientManagerCoverage() {
        Tenant tenant = createTenant("assignment-validity");

        AppUser administrator = createUser(tenant, "Validity Administrator", UserRole.TENANT_ADMIN);

        AppUser manager = createUser(tenant, "Validity Manager", UserRole.TENANT_MANAGER);

        AppUser member = createUser(tenant, "Validity Member", UserRole.TENANT_USER);

        OrganizationalUnit unit =
                createUnit(
                        tenant,
                        null,
                        "Validity Unit",
                        "VALIDITY-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        Instant start = Instant.now();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                request(
                                        member.getId(),
                                        unit.getId(),
                                        null,
                                        "Invalid Validity",
                                        false,
                                        start,
                                        start)));

        Instant managerEnd = start.plus(5, ChronoUnit.DAYS);

        OrganizationAssignmentResponse managerAssignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                manager.getId(),
                                unit.getId(),
                                null,
                                "Temporary Manager",
                                true,
                                start,
                                managerEnd));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                request(
                                        member.getId(),
                                        unit.getId(),
                                        managerAssignment.id(),
                                        "Permanent Subordinate",
                                        false,
                                        start.plus(1, ChronoUnit.DAYS),
                                        null)));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                request(
                                        member.getId(),
                                        unit.getId(),
                                        managerAssignment.id(),
                                        "Longer Subordinate",
                                        false,
                                        start.plus(1, ChronoUnit.DAYS),
                                        managerEnd.plus(1, ChronoUnit.DAYS))));
    }

    @Test
    void rejectsSelfReportingAssignment() {
        Tenant tenant = createTenant("assignment-self-reporting");

        AppUser administrator =
                createUser(tenant, "Self Reporting Administrator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Self Reporting Member", UserRole.TENANT_USER);

        OrganizationalUnit unit =
                createUnit(
                        tenant,
                        null,
                        "Self Reporting Unit",
                        "SELF-REPORTING-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        Instant validFrom = Instant.now();

        OrganizationAssignmentResponse existingAssignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                member.getId(),
                                unit.getId(),
                                null,
                                "Primary Position",
                                true,
                                validFrom,
                                null));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationAssignmentService.createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                request(
                                        member.getId(),
                                        unit.getId(),
                                        existingAssignment.id(),
                                        "Self Reporting Position",
                                        false,
                                        validFrom,
                                        null)));
    }

    @Test
    void deactivationRemovesAssignmentFromEffectiveResults() {
        Tenant tenant = createTenant("assignment-deactivation");

        AppUser administrator =
                createUser(tenant, "Deactivation Administrator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Deactivation Member", UserRole.TENANT_USER);

        OrganizationalUnit unit =
                createUnit(
                        tenant,
                        null,
                        "Deactivation Unit",
                        "DEACTIVATION-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        Instant validFrom = Instant.now().minus(1, ChronoUnit.DAYS);

        OrganizationAssignmentResponse assignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                member.getId(),
                                unit.getId(),
                                null,
                                "Active Position",
                                true,
                                validFrom,
                                null));

        List<OrganizationAssignmentResponse> effectiveBeforeDeactivation =
                organizationAssignmentService.getEffectiveUserAssignments(
                        tenant.getId(), member.getId(), Instant.now());

        assertEquals(1, effectiveBeforeDeactivation.size());

        OrganizationAssignmentResponse deactivatedAssignment =
                organizationAssignmentService.deactivateAssignment(tenant.getId(), assignment.id());

        assertEquals(OrganizationAssignmentStatus.INACTIVE, deactivatedAssignment.status());

        assertNotNull(deactivatedAssignment.validUntil());

        List<OrganizationAssignmentResponse> effectiveAfterDeactivation =
                organizationAssignmentService.getEffectiveUserAssignments(
                        tenant.getId(), member.getId(), Instant.now());

        assertTrue(effectiveAfterDeactivation.isEmpty());
    }

    @Test
    void preventsManagerDeactivationWithActiveDirectReports() {
        Tenant tenant = createTenant("assignment-manager-deactivation");

        AppUser administrator =
                createUser(tenant, "Manager Deactivation Administrator", UserRole.TENANT_ADMIN);

        AppUser manager =
                createUser(tenant, "Manager Deactivation Manager", UserRole.TENANT_MANAGER);

        AppUser member = createUser(tenant, "Manager Deactivation Member", UserRole.TENANT_USER);

        OrganizationalUnit unit =
                createUnit(
                        tenant,
                        null,
                        "Manager Deactivation Unit",
                        "MANAGER-DEACTIVATION-UNIT",
                        OrganizationalUnitType.DEPARTMENT);

        Instant validFrom = Instant.now().minus(1, ChronoUnit.DAYS);

        OrganizationAssignmentResponse managerAssignment =
                organizationAssignmentService.createAssignment(
                        tenant.getId(),
                        administrator.getId(),
                        request(
                                manager.getId(),
                                unit.getId(),
                                null,
                                "Manager",
                                true,
                                validFrom,
                                null));

        organizationAssignmentService.createAssignment(
                tenant.getId(),
                administrator.getId(),
                request(
                        member.getId(),
                        unit.getId(),
                        managerAssignment.id(),
                        "Direct Report",
                        true,
                        validFrom,
                        null));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        organizationAssignmentService.deactivateAssignment(
                                tenant.getId(), managerAssignment.id()));

        OrganizationAssignmentResponse unchangedManagerAssignment =
                organizationAssignmentService.getAssignment(tenant.getId(), managerAssignment.id());

        assertEquals(OrganizationAssignmentStatus.ACTIVE, unchangedManagerAssignment.status());
    }

    private OrganizationAssignmentCreateRequest request(
            UUID userId,
            UUID organizationalUnitId,
            UUID reportsToAssignmentId,
            String positionTitle,
            boolean primaryAssignment,
            Instant validFrom,
            Instant validUntil) {
        return new OrganizationAssignmentCreateRequest(
                userId,
                organizationalUnitId,
                reportsToAssignmentId,
                positionTitle,
                primaryAssignment,
                validFrom,
                validUntil);
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

    private AppUser createUser(Tenant tenant, String fullName, UserRole role) {
        String suffix = uniqueSuffix();

        AppUser user =
                new AppUser(
                        tenant,
                        fullName,
                        role.name().toLowerCase().replace('_', '.')
                                + "."
                                + suffix
                                + "@example.test",
                        "test-password-hash",
                        role);

        return appUserRepository.saveAndFlush(user);
    }

    private Tenant createTenant(String prefix) {
        String suffix = uniqueSuffix();

        Tenant tenant = new Tenant(prefix + " Tenant", prefix + "-" + suffix);

        return tenantRepository.saveAndFlush(tenant);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
