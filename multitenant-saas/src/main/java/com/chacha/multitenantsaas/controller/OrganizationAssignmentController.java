package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentResponse;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentUserOptionResponse;
import com.chacha.multitenantsaas.service.OrganizationAssignmentCommandService;
import com.chacha.multitenantsaas.service.OrganizationAssignmentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}" + "/organization/assignments")
public class OrganizationAssignmentController {

    private final OrganizationAssignmentService organizationAssignmentService;

    private final OrganizationAssignmentCommandService organizationAssignmentCommandService;

    public OrganizationAssignmentController(
            OrganizationAssignmentService organizationAssignmentService,
            OrganizationAssignmentCommandService organizationAssignmentCommandService) {
        this.organizationAssignmentService = organizationAssignmentService;

        this.organizationAssignmentCommandService = organizationAssignmentCommandService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".canCreateOrganizationAssignment("
                    + "#tenantId,"
                    + "#request.organizationalUnitId(),"
                    + "'organization.assignment.manage'"
                    + ")")
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationAssignmentResponse>> createAssignment(
            @PathVariable UUID tenantId,
            @Valid @RequestBody OrganizationAssignmentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        OrganizationAssignmentResponse response =
                organizationAssignmentCommandService.createAssignment(tenantId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational assignment " + "created successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".canCreateOrganizationAssignment("
                    + "#tenantId,"
                    + "#organizationalUnitId,"
                    + "'organization.assignment.manage'"
                    + ")")
    @GetMapping("/units/{organizationalUnitId}/user-options")
    public ResponseEntity<ApiResponse<List<OrganizationAssignmentUserOptionResponse>>>
            getAssignableUsers(
                    @PathVariable UUID tenantId, @PathVariable UUID organizationalUnitId) {
        List<OrganizationAssignmentUserOptionResponse> response =
                organizationAssignmentService.getAssignableUsers(tenantId, organizationalUnitId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Assignable organization users " + "fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".canAccessOrganizationAssignment("
                    + "#tenantId,"
                    + "#assignmentId,"
                    + "'organization.assignment.read'"
                    + ")")
    @GetMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<OrganizationAssignmentResponse>> getAssignment(
            @PathVariable UUID tenantId, @PathVariable UUID assignmentId) {
        OrganizationAssignmentResponse response =
                organizationAssignmentService.getAssignment(tenantId, assignmentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational assignment " + "fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasUserPermission("
                    + "#tenantId,"
                    + "#userId,"
                    + "'organization.assignment.read'"
                    + ")")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<OrganizationAssignmentResponse>>> getUserAssignments(
            @PathVariable UUID tenantId, @PathVariable UUID userId) {
        List<OrganizationAssignmentResponse> response =
                organizationAssignmentService.getUserAssignments(tenantId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User organizational assignments " + "fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasUserPermission("
                    + "#tenantId,"
                    + "#userId,"
                    + "'organization.assignment.read'"
                    + ")")
    @GetMapping("/users/{userId}/effective")
    public ResponseEntity<ApiResponse<List<OrganizationAssignmentResponse>>>
            getEffectiveUserAssignments(
                    @PathVariable UUID tenantId,
                    @PathVariable UUID userId,
                    @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                            Instant effectiveAt) {
        List<OrganizationAssignmentResponse> response =
                organizationAssignmentService.getEffectiveUserAssignments(
                        tenantId, userId, effectiveAt);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Effective organizational " + "assignments fetched " + "successfully",
                        response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalUnitPermission("
                    + "#tenantId,"
                    + "#organizationalUnitId,"
                    + "'organization.assignment.read'"
                    + ")")
    @GetMapping("/units/{organizationalUnitId}")
    public ResponseEntity<ApiResponse<List<OrganizationAssignmentResponse>>> getUnitAssignments(
            @PathVariable UUID tenantId, @PathVariable UUID organizationalUnitId) {
        List<OrganizationAssignmentResponse> response =
                organizationAssignmentService.getUnitAssignments(tenantId, organizationalUnitId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit assignments " + "fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".canReadDirectReportsAssignments("
                    + "#tenantId,"
                    + "#managerAssignmentId,"
                    + "'organization.assignment.read'"
                    + ")")
    @GetMapping("/{managerAssignmentId}/direct-reports")
    public ResponseEntity<ApiResponse<List<OrganizationAssignmentResponse>>> getDirectReports(
            @PathVariable UUID tenantId, @PathVariable UUID managerAssignmentId) {
        List<OrganizationAssignmentResponse> response =
                organizationAssignmentService.getDirectReports(tenantId, managerAssignmentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Direct-report assignments " + "fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".canAccessOrganizationAssignment("
                    + "#tenantId,"
                    + "#assignmentId,"
                    + "'organization.assignment.manage'"
                    + ")")
    @PatchMapping("/{assignmentId}/deactivate")
    public ResponseEntity<ApiResponse<OrganizationAssignmentResponse>> deactivateAssignment(
            @PathVariable UUID tenantId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal Jwt jwt) {
        OrganizationAssignmentResponse response =
                organizationAssignmentCommandService.deactivateAssignment(
                        tenantId, assignmentId, jwt);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational assignment " + "deactivated successfully", response));
    }
}
