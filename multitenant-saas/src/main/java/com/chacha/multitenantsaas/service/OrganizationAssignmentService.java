package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OrganizationAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentResponse;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentUserOptionResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationAssignmentService {

    private final TenantLookupService tenantLookupService;

    private final AppUserRepository appUserRepository;

    private final OrganizationalUnitRepository organizationalUnitRepository;

    private final UserOrganizationAssignmentRepository assignmentRepository;

    public OrganizationAssignmentService(
            TenantLookupService tenantLookupService,
            AppUserRepository appUserRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            UserOrganizationAssignmentRepository assignmentRepository) {
        this.tenantLookupService = tenantLookupService;

        this.appUserRepository = appUserRepository;

        this.organizationalUnitRepository = organizationalUnitRepository;

        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationAssignmentUserOptionResponse> getAssignableUsers(
            UUID tenantId, UUID organizationalUnitId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getRequiredActiveUnit(tenantId, organizationalUnitId);

        return appUserRepository.findByTenantId(tenantId).stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .sorted(
                        Comparator.comparing(AppUser::getFullName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(AppUser::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(
                        user ->
                                new OrganizationAssignmentUserOptionResponse(
                                        user.getId(), user.getFullName(), user.getEmail()))
                .toList();
    }

    @Transactional
    public OrganizationAssignmentResponse createAssignment(
            UUID tenantId, UUID createdByUserId, OrganizationAssignmentCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Organization assignment request " + "is required.");
        }

        if (request.userId() == null) {
            throw new IllegalArgumentException("Assigned user id is required.");
        }

        if (request.organizationalUnitId() == null) {
            throw new IllegalArgumentException("Organizational unit id is required.");
        }

        if (createdByUserId == null) {
            throw new IllegalArgumentException("Creating user id is required.");
        }

        Tenant tenant = tenantLookupService.getActiveByIdOrThrow(tenantId);

        AppUser assignedUser = getRequiredActiveUser(tenantId, request.userId(), "Assigned user");

        AppUser createdByUser = getRequiredActiveUser(tenantId, createdByUserId, "Creating user");

        OrganizationalUnit organizationalUnit =
                getRequiredActiveUnit(tenantId, request.organizationalUnitId());

        Instant validFrom =
                normalizeDatabaseInstant(
                        request.validFrom() == null ? Instant.now() : request.validFrom());

        Instant validUntil = normalizeDatabaseInstant(request.validUntil());

        validateValidityRange(validFrom, validUntil);

        if (request.primaryAssignment()) {
            validatePrimaryAssignmentAvailability(
                    tenantId, assignedUser.getId(), validFrom, validUntil);
        }

        UserOrganizationAssignment reportsToAssignment =
                resolveReportingAssignment(
                        tenantId,
                        assignedUser,
                        request.reportsToAssignmentId(),
                        validFrom,
                        validUntil);

        String positionTitle = normalizePositionTitle(request.positionTitle());

        UserOrganizationAssignment assignment =
                new UserOrganizationAssignment(
                        tenant,
                        assignedUser,
                        organizationalUnit,
                        reportsToAssignment,
                        positionTitle,
                        request.primaryAssignment(),
                        validFrom,
                        validUntil,
                        createdByUser);

        UserOrganizationAssignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        return mapToResponse(savedAssignment);
    }

    @Transactional(readOnly = true)
    public OrganizationAssignmentResponse getAssignment(UUID tenantId, UUID assignmentId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        return mapToResponse(getAssignmentOrThrow(tenantId, assignmentId));
    }

    @Transactional(readOnly = true)
    public List<OrganizationAssignmentResponse> getUserAssignments(UUID tenantId, UUID userId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getUserOrThrow(tenantId, userId, "User");

        return assignmentRepository.findUserAssignments(tenantId, userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationAssignmentResponse> getEffectiveUserAssignments(
            UUID tenantId, UUID userId, Instant effectiveAt) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getUserOrThrow(tenantId, userId, "User");

        Instant resolvedEffectiveAt = effectiveAt == null ? Instant.now() : effectiveAt;

        return assignmentRepository
                .findEffectiveAssignmentsForUser(
                        tenantId, userId, OrganizationAssignmentStatus.ACTIVE, resolvedEffectiveAt)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationAssignmentResponse> getUnitAssignments(
            UUID tenantId, UUID organizationalUnitId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getUnitOrThrow(tenantId, organizationalUnitId);

        return assignmentRepository.findUnitAssignments(tenantId, organizationalUnitId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationAssignmentResponse> getDirectReports(
            UUID tenantId, UUID managerAssignmentId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        getAssignmentOrThrow(tenantId, managerAssignmentId);

        return assignmentRepository.findDirectReports(tenantId, managerAssignmentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OrganizationAssignmentResponse deactivateAssignment(UUID tenantId, UUID assignmentId) {
        tenantLookupService.getActiveByIdOrThrow(tenantId);

        UserOrganizationAssignment assignment = getAssignmentOrThrow(tenantId, assignmentId);

        if (assignment.getStatus() == OrganizationAssignmentStatus.INACTIVE) {
            return mapToResponse(assignment);
        }

        long activeDirectReportCount =
                assignmentRepository.countByTenant_IdAndReportsToAssignment_IdAndStatus(
                        tenantId, assignmentId, OrganizationAssignmentStatus.ACTIVE);

        if (activeDirectReportCount > 0) {
            throw new IllegalArgumentException(
                    "Assignment cannot be deactivated "
                            + "while active assignments "
                            + "report to it.");
        }

        Instant now = Instant.now();

        assignment.setStatus(OrganizationAssignmentStatus.INACTIVE);

        if (assignment.getValidFrom() != null
                && now.isAfter(assignment.getValidFrom())
                && (assignment.getValidUntil() == null
                        || assignment.getValidUntil().isAfter(now))) {
            assignment.setValidUntil(now);
        }

        UserOrganizationAssignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        return mapToResponse(savedAssignment);
    }

    private void validatePrimaryAssignmentAvailability(
            UUID tenantId, UUID userId, Instant validFrom, Instant validUntil) {

        long overlappingAssignmentCount;

        if (validUntil == null) {
            overlappingAssignmentCount =
                    assignmentRepository.countOverlappingOpenEndedPrimaryAssignments(
                            tenantId, userId, OrganizationAssignmentStatus.ACTIVE, validFrom);
        } else {
            overlappingAssignmentCount =
                    assignmentRepository.countOverlappingBoundedPrimaryAssignments(
                            tenantId,
                            userId,
                            OrganizationAssignmentStatus.ACTIVE,
                            validFrom,
                            validUntil);
        }

        if (overlappingAssignmentCount > 0) {
            throw new DuplicateResourceException(
                    "User already has an overlapping "
                            + "active primary organizational "
                            + "assignment.");
        }
    }

    private UserOrganizationAssignment resolveReportingAssignment(
            UUID tenantId,
            AppUser assignedUser,
            UUID reportsToAssignmentId,
            Instant validFrom,
            Instant validUntil) {
        if (reportsToAssignmentId == null) {
            return null;
        }

        UserOrganizationAssignment reportsToAssignment =
                getAssignmentOrThrow(tenantId, reportsToAssignmentId);

        if (reportsToAssignment.getStatus() != OrganizationAssignmentStatus.ACTIVE) {
            throw new IllegalArgumentException("Reporting assignment must be active.");
        }

        if (reportsToAssignment.getUser().getId().equals(assignedUser.getId())) {
            throw new IllegalArgumentException(
                    "A user cannot report to their own " + "organizational assignment.");
        }

        if (reportsToAssignment.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Reporting assignment user " + "must be active.");
        }

        if (reportsToAssignment.getOrganizationalUnit().getStatus()
                != OrganizationalUnitStatus.ACTIVE) {
            throw new IllegalArgumentException("Reporting assignment unit " + "must be active.");
        }

        if (reportsToAssignment.getValidFrom().isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "Reporting assignment must begin "
                            + "on or before the subordinate "
                            + "assignment.");
        }

        Instant managerValidUntil = reportsToAssignment.getValidUntil();

        if (managerValidUntil != null) {
            if (validUntil == null || managerValidUntil.isBefore(validUntil)) {
                throw new IllegalArgumentException(
                        "Reporting assignment must remain "
                                + "valid for the complete "
                                + "subordinate assignment "
                                + "period.");
            }
        }

        return reportsToAssignment;
    }

    private void validateValidityRange(Instant validFrom, Instant validUntil) {
        if (validFrom == null) {
            throw new IllegalArgumentException("Assignment valid-from time " + "is required.");
        }

        if (validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "Assignment valid-until time " + "must be after valid-from time.");
        }
    }

    private String normalizePositionTitle(String positionTitle) {
        if (positionTitle == null) {
            return null;
        }

        String normalizedTitle = positionTitle.trim();

        if (normalizedTitle.isEmpty()) {
            return null;
        }

        if (normalizedTitle.length() > 150) {
            throw new IllegalArgumentException(
                    "Position title must not exceed " + "150 characters.");
        }

        return normalizedTitle;
    }

    private AppUser getRequiredActiveUser(UUID tenantId, UUID userId, String description) {
        AppUser user = getUserOrThrow(tenantId, userId, description);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(description + " must be active.");
        }

        return user;
    }

    private AppUser getUserOrThrow(UUID tenantId, UUID userId, String description) {
        if (userId == null) {
            throw new IllegalArgumentException(description + " id is required.");
        }

        return appUserRepository
                .findByTenantIdAndId(tenantId, userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        description + " not found with id: " + userId));
    }

    private OrganizationalUnit getRequiredActiveUnit(UUID tenantId, UUID organizationalUnitId) {
        OrganizationalUnit unit = getUnitOrThrow(tenantId, organizationalUnitId);

        if (unit.getStatus() != OrganizationalUnitStatus.ACTIVE) {
            throw new IllegalArgumentException("Organizational unit must be active.");
        }

        return unit;
    }

    private OrganizationalUnit getUnitOrThrow(UUID tenantId, UUID organizationalUnitId) {
        if (organizationalUnitId == null) {
            throw new IllegalArgumentException("Organizational unit id is required.");
        }

        return organizationalUnitRepository
                .findByTenant_IdAndId(tenantId, organizationalUnitId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Organizational unit "
                                                + "not found with id: "
                                                + organizationalUnitId));
    }

    private UserOrganizationAssignment getAssignmentOrThrow(UUID tenantId, UUID assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("Organization assignment id " + "is required.");
        }

        return assignmentRepository
                .findByTenant_IdAndId(tenantId, assignmentId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Organization assignment "
                                                + "not found with id: "
                                                + assignmentId));
    }

    private Instant normalizeDatabaseInstant(Instant value) {
        if (value == null) {
            return null;
        }

        return value.truncatedTo(ChronoUnit.MICROS);
    }

    private OrganizationAssignmentResponse mapToResponse(UserOrganizationAssignment assignment) {
        UserOrganizationAssignment managerAssignment = assignment.getReportsToAssignment();

        UUID managerUserId = managerAssignment == null ? null : managerAssignment.getUser().getId();

        String managerUserFullName =
                managerAssignment == null ? null : managerAssignment.getUser().getFullName();

        return new OrganizationAssignmentResponse(
                assignment.getId(),
                assignment.getTenant().getId(),
                assignment.getUser().getId(),
                assignment.getUser().getFullName(),
                assignment.getOrganizationalUnit().getId(),
                assignment.getOrganizationalUnit().getName(),
                managerAssignment == null ? null : managerAssignment.getId(),
                managerUserId,
                managerUserFullName,
                assignment.getPositionTitle(),
                assignment.isPrimaryAssignment(),
                assignment.getStatus(),
                assignment.getValidFrom(),
                assignment.getValidUntil(),
                assignment.getCreatedByUser().getId(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }
}
