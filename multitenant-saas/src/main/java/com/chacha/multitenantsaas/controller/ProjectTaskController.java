package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.service.ProjectTaskService;
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
@RequestMapping(
        "/api/tenants/{tenantId}"
                + "/projects/{projectId}/tasks"
)
public class ProjectTaskController {

    private final ProjectTaskService projectTaskService;

    public ProjectTaskController(
            ProjectTaskService projectTaskService
    ) {
        this.projectTaskService = projectTaskService;
    }

    @PreAuthorize(
            "@projectSecurity.canManageTasks(#tenantId, #projectId)"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectTaskResponse>>
    createTask(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody
            ProjectTaskCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectTaskResponse response =
                projectTaskService.createTask(
                        tenantId,
                        projectId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task created successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@projectSecurity.canReadTasks(#tenantId, #projectId)"
    )
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<ProjectTaskResponse>>
            >
    getTasks(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int size,
            @RequestParam(defaultValue = "createdAt")
            String sortBy,
            @RequestParam(defaultValue = "desc")
            String sortDir,
            @RequestParam(required = false)
            ProjectTaskStatus status,
            @RequestParam(required = false)
            ProjectTaskPriority priority,
            @RequestParam(required = false)
            UUID assigneeUserId,
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
                        "title",
                        "status",
                        "priority",
                        "dueAt"
                )
        );

        PageResponse<ProjectTaskResponse> response =
                projectTaskService.getTasks(
                        tenantId,
                        projectId,
                        status,
                        priority,
                        assigneeUserId,
                        search,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tasks fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@projectSecurity.canReadTasks(#tenantId, #projectId)"
    )
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<ProjectTaskResponse>>
    getTask(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId
    ) {
        ProjectTaskResponse response =
                projectTaskService.getTask(
                        tenantId,
                        projectId,
                        taskId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@projectSecurity.canManageTasks(#tenantId, #projectId)"
    )
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<ProjectTaskResponse>>
    updateTask(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody
            ProjectTaskUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectTaskResponse response =
                projectTaskService.updateTask(
                        tenantId,
                        projectId,
                        taskId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task updated successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@projectSecurity.canUpdateTaskStatus("
                    + "#tenantId, #projectId, #taskId)"
    )
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<ProjectTaskResponse>>
    updateTaskStatus(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody
            ProjectTaskStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectTaskResponse response =
                projectTaskService.updateTaskStatus(
                        tenantId,
                        projectId,
                        taskId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task status updated successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@projectSecurity.canManageTasks(#tenantId, #projectId)"
    )
    @PatchMapping("/{taskId}/assignee")
    public ResponseEntity<ApiResponse<ProjectTaskResponse>>
    updateTaskAssignee(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestBody
            ProjectTaskAssigneeUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectTaskResponse response =
                projectTaskService.updateAssignee(
                        tenantId,
                        projectId,
                        taskId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task assignee updated successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@projectSecurity.canManageTasks(#tenantId, #projectId)"
    )
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<ProjectTaskResponse>>
    cancelTask(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectTaskResponse response =
                projectTaskService.cancelTask(
                        tenantId,
                        projectId,
                        taskId,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task cancelled successfully",
                        response
                )
        );
    }
}