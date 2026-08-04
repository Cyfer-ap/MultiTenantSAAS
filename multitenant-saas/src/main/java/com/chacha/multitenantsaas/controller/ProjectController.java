package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(
            ProjectService projectService
    ) {
        this.projectService = projectService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'project.create'"
                    + ")"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>>
    createProject(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectResponse response =
                projectService.createProject(
                        tenantId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project created successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'project.read'"
                    + ")"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>>
    getProjects(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int size,
            @RequestParam(defaultValue = "createdAt")
            String sortBy,
            @RequestParam(defaultValue = "desc")
            String sortDir,
            @RequestParam(required = false)
            ProjectStatus status,
            @RequestParam(required = false)
            String search
    ) {
        Pageable pageable = PageRequest.of(
                PaginationUtils.validatePage(page),
                PaginationUtils.validateSize(size),
                SortingUtils.getDirection(sortDir),
                SortingUtils.validateSortBy(
                        sortBy,
                        "createdAt",
                        "createdAt",
                        "updatedAt",
                        "name",
                        "status"
                )
        );

        PageResponse<ProjectResponse> response =
                projectService.getProjects(
                        tenantId,
                        status,
                        search,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Projects fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.read'"
                    + ")"
    )
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>>
    getProject(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId
    ) {
        ProjectResponse response =
                projectService.getProject(
                        tenantId,
                        projectId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.update'"
                    + ")"
    )
    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>>
    updateProject(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectResponse response =
                projectService.updateProject(
                        tenantId,
                        projectId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project updated successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.update'"
                    + ")"
    )
    @PatchMapping("/{projectId}/status")
    public ResponseEntity<ApiResponse<ProjectResponse>>
    updateProjectStatus(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody
            ProjectStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectResponse response =
                projectService.updateProjectStatus(
                        tenantId,
                        projectId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project status updated successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasProjectPermission("
                    + "#tenantId,"
                    + "#projectId,"
                    + "'project.archive'"
                    + ")"
    )
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>>
    archiveProject(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectResponse response =
                projectService.archiveProject(
                        tenantId,
                        projectId,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project archived successfully",
                        response
                )
        );
    }
}