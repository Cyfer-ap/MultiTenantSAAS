package com.chacha.multitenantsaas.tasktemplates;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/task-templates")
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;

    public TaskTemplateController(TaskTemplateService taskTemplateService) {
        this.taskTemplateService = taskTemplateService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TaskTemplateDtos.Response>>> list(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task templates fetched successfully",
                        taskTemplateService.list(tenantId, projectId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponse<TaskTemplateDtos.Response>> get(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task template fetched successfully",
                        taskTemplateService.get(tenantId, projectId, templateId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskTemplateDtos.Response>> create(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody TaskTemplateDtos.UpsertRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task template created successfully",
                        taskTemplateService.create(tenantId, projectId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponse<TaskTemplateDtos.Response>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID templateId,
            @Valid @RequestBody TaskTemplateDtos.UpsertRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task template updated successfully",
                        taskTemplateService.update(tenantId, projectId, templateId, request)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID templateId) {
        taskTemplateService.delete(tenantId, projectId, templateId);
        return ResponseEntity.ok(ApiResponse.success("Task template deleted successfully", null));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{templateId}/instantiate")
    public ResponseEntity<ApiResponse<TaskTemplateDtos.InstantiateResponse>> instantiate(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task created from template successfully",
                        taskTemplateService.instantiate(tenantId, projectId, templateId, jwt)));
    }
}
