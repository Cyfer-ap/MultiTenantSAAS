package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse;
import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse.ScopeTargetOption;
import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse.UserOption;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationAssignmentReferenceDataService {

    private final TenantLookupService tenantLookupService;
    private final AppUserRepository appUserRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final ProjectRepository projectRepository;
    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;

    public AuthorizationAssignmentReferenceDataService(
            TenantLookupService tenantLookupService,
            AppUserRepository appUserRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            ProjectRepository projectRepository,
            UserOrganizationAssignmentRepository userOrganizationAssignmentRepository) {
        this.tenantLookupService = tenantLookupService;
        this.appUserRepository = appUserRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.projectRepository = projectRepository;
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public AuthorizationAssignmentReferenceDataResponse getReferenceData(UUID tenantId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        List<UserOption> users =
                appUserRepository.findByTenantId(tenantId).stream()
                        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                        .sorted(
                                Comparator.comparing(
                                                AppUser::getFullName, String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(
                                                AppUser::getEmail, String.CASE_INSENSITIVE_ORDER))
                        .map(
                                user ->
                                        new UserOption(
                                                user.getId(), user.getFullName(), user.getEmail()))
                        .toList();

        List<ScopeTargetOption> organizationalUnits =
                organizationalUnitRepository.findAllByTenant_IdOrderByNameAsc(tenantId).stream()
                        .filter(unit -> unit.getStatus() == OrganizationalUnitStatus.ACTIVE)
                        .map(this::mapOrganizationalUnit)
                        .toList();

        List<ScopeTargetOption> projects =
                projectRepository.findAllByTenant_IdOrderByNameAsc(tenantId).stream()
                        .filter(project -> project.getStatus() != ProjectStatus.ARCHIVED)
                        .map(this::mapProject)
                        .toList();

        List<ScopeTargetOption> directReportsAnchors =
                userOrganizationAssignmentRepository
                        .findTenantAssignmentsByStatus(
                                tenantId, OrganizationAssignmentStatus.ACTIVE)
                        .stream()
                        .filter(assignment -> assignment.getUser().getStatus() == UserStatus.ACTIVE)
                        .filter(
                                assignment ->
                                        assignment.getOrganizationalUnit().getStatus()
                                                == OrganizationalUnitStatus.ACTIVE)
                        .map(this::mapDirectReportsAnchor)
                        .toList();

        return new AuthorizationAssignmentReferenceDataResponse(
                users, organizationalUnits, projects, directReportsAnchors);
    }

    private ScopeTargetOption mapOrganizationalUnit(OrganizationalUnit unit) {
        String description = unit.getType().name().replace('_', ' ');

        if (unit.getCode() != null && !unit.getCode().isBlank()) {
            description += " • " + unit.getCode();
        }

        return new ScopeTargetOption(unit.getId(), unit.getName(), description, null);
    }

    private ScopeTargetOption mapProject(Project project) {
        return new ScopeTargetOption(
                project.getId(),
                project.getName(),
                project.getStatus().name().replace('_', ' '),
                null);
    }

    private ScopeTargetOption mapDirectReportsAnchor(UserOrganizationAssignment assignment) {
        OrganizationalUnit unit = assignment.getOrganizationalUnit();

        String label = unit.getName();

        if (assignment.getPositionTitle() != null && !assignment.getPositionTitle().isBlank()) {
            label += " — " + assignment.getPositionTitle();
        }

        String description =
                assignment.isPrimaryAssignment()
                        ? "Primary organizational assignment"
                        : "Additional organizational assignment";

        return new ScopeTargetOption(
                assignment.getId(), label, description, assignment.getUser().getId());
    }
}
