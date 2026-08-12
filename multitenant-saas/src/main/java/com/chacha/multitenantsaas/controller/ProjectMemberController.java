package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import com.chacha.multitenantsaas.service.ProjectMemberService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/{tenantId}" + "/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.member.manage'"
                    + ")")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectMemberAddRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ProjectMemberResponse response =
                projectMemberService.addMember(tenantId, projectId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Project member added successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.read'"
                    + ")")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProjectMemberResponse>>> getMembers(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) ProjectMemberRole role,
            @RequestParam(required = false) String search) {
        Pageable pageable =
                PageRequest.of(
                        PaginationUtils.validatePage(page),
                        PaginationUtils.validateSize(size),
                        SortingUtils.getDirection(sortDir),
                        SortingUtils.validateSortBy(
                                sortBy, "assignedAt", "assignedAt", "updatedAt", "role"));

        PageResponse<ProjectMemberResponse> response =
                projectMemberService.getMembers(tenantId, projectId, role, search, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Project members fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.read'"
                    + ")")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> getMember(
            @PathVariable UUID tenantId, @PathVariable UUID projectId, @PathVariable UUID userId) {
        ProjectMemberResponse response =
                projectMemberService.getMember(tenantId, projectId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Project member fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.member.manage'"
                    + ")")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody ProjectMemberRoleUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ProjectMemberResponse response =
                projectMemberService.updateMemberRole(tenantId, projectId, userId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Project member role updated successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.member.manage'"
                    + ")")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> removeMember(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        ProjectMemberResponse response =
                projectMemberService.removeMember(tenantId, projectId, userId, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Project member removed successfully", response));
    }
}
