package com.chacha.multitenantsaas.recurringwork;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/recurring-work")
public class RecurringTaskController {

    private final RecurringTaskService recurringTaskService;

    public RecurringTaskController(RecurringTaskService recurringTaskService) {
        this.recurringTaskService = recurringTaskService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecurringTaskDtos.Response>>> list(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Recurring work fetched successfully",
                        recurringTaskService.list(tenantId, projectId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{definitionId}")
    public ResponseEntity<ApiResponse<RecurringTaskDtos.Response>> get(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Recurring work fetched successfully",
                        recurringTaskService.get(tenantId, projectId, definitionId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTaskDtos.Response>> create(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody RecurringTaskDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Recurring work created successfully",
                        recurringTaskService.create(tenantId, projectId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/{definitionId}")
    public ResponseEntity<ApiResponse<RecurringTaskDtos.Response>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId,
            @Valid @RequestBody RecurringTaskDtos.UpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Recurring work updated successfully",
                        recurringTaskService.update(tenantId, projectId, definitionId, request)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{definitionId}/pause")
    public ResponseEntity<ApiResponse<RecurringTaskDtos.Response>> pause(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Recurring work paused successfully",
                        recurringTaskService.pause(tenantId, projectId, definitionId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{definitionId}/resume")
    public ResponseEntity<ApiResponse<RecurringTaskDtos.Response>> resume(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Recurring work resumed successfully",
                        recurringTaskService.resume(tenantId, projectId, definitionId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{definitionId}/occurrences")
    public ResponseEntity<ApiResponse<PageResponse<RecurringTaskDtos.OccurrenceResponse>>> occurrences(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Recurring task occurrences fetched successfully",
                        recurringTaskService.occurrences(
                                tenantId, projectId, definitionId, pageable)));
    }
}
