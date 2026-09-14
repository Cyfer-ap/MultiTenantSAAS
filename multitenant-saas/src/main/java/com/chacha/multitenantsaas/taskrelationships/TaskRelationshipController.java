package com.chacha.multitenantsaas.taskrelationships;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.taskrelationships.dto.ProjectTaskLabelRequest;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskDependencyCreateRequest;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskLabelResponse;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskParentUpdateRequest;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskRelationshipsResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}")
public class TaskRelationshipController {

    private final TaskRelationshipQueryService queryService;
    private final TaskGraphService graphService;
    private final TaskLabelService labelService;

    public TaskRelationshipController(
            TaskRelationshipQueryService queryService,
            TaskGraphService graphService,
            TaskLabelService labelService) {
        this.queryService = queryService;
        this.graphService = graphService;
        this.labelService = labelService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/tasks/{taskId}/relationships")
    public ResponseEntity<ApiResponse<TaskRelationshipsResponse>> getRelationships(
            @PathVariable UUID tenantId, @PathVariable UUID projectId, @PathVariable UUID taskId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task relationships fetched successfully",
                        queryService.getRelationships(tenantId, projectId, taskId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/tasks/{taskId}/parent")
    public ResponseEntity<ApiResponse<TaskRelationshipsResponse>> updateParent(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestBody TaskParentUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task parent updated successfully",
                        graphService.updateParent(
                                tenantId, projectId, taskId, request.parentTaskId(), jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/tasks/{taskId}/dependencies")
    public ResponseEntity<ApiResponse<TaskRelationshipsResponse>> addDependency(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskDependencyCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task dependency added successfully",
                        graphService.addDependency(
                                tenantId, projectId, taskId, request.blockingTaskId(), jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @DeleteMapping("/tasks/{taskId}/dependencies/{blockingTaskId}")
    public ResponseEntity<ApiResponse<TaskRelationshipsResponse>> removeDependency(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID blockingTaskId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task dependency removed successfully",
                        graphService.removeDependency(
                                tenantId, projectId, taskId, blockingTaskId, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/task-labels")
    public ResponseEntity<ApiResponse<List<TaskLabelResponse>>> listLabels(
            @PathVariable UUID tenantId, @PathVariable UUID projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task labels fetched successfully",
                        labelService.listLabels(tenantId, projectId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/task-labels")
    public ResponseEntity<ApiResponse<TaskLabelResponse>> createLabel(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectTaskLabelRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task label created successfully",
                        labelService.createLabel(tenantId, projectId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/task-labels/{labelId}")
    public ResponseEntity<ApiResponse<TaskLabelResponse>> updateLabel(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID labelId,
            @Valid @RequestBody ProjectTaskLabelRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task label updated successfully",
                        labelService.updateLabel(tenantId, projectId, labelId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @DeleteMapping("/task-labels/{labelId}")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID labelId,
            @AuthenticationPrincipal Jwt jwt) {
        labelService.deleteLabel(tenantId, projectId, labelId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Task label deleted successfully", null));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<ApiResponse<TaskRelationshipsResponse>> assignLabel(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task label assigned successfully",
                        labelService.assignLabel(tenantId, projectId, taskId, labelId, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<ApiResponse<TaskRelationshipsResponse>> unassignLabel(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task label removed successfully",
                        labelService.unassignLabel(tenantId, projectId, taskId, labelId, jwt)));
    }
}
