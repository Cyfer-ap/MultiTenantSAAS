package com.chacha.multitenantsaas.forms;

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
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/forms")
public class FormController {

    private final FormDefinitionService definitionService;
    private final FormSubmissionService submissionService;

    public FormController(
            FormDefinitionService definitionService, FormSubmissionService submissionService) {
        this.definitionService = definitionService;
        this.submissionService = submissionService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FormDtos.SummaryResponse>>> list(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Forms fetched successfully",
                        definitionService.list(tenantId, projectId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{formId}")
    public ResponseEntity<ApiResponse<FormDtos.Response>> get(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID formId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Form fetched successfully",
                        definitionService.get(tenantId, projectId, formId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<FormDtos.Response>> create(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody FormDtos.UpsertRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Form created successfully",
                        definitionService.create(tenantId, projectId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/{formId}")
    public ResponseEntity<ApiResponse<FormDtos.Response>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID formId,
            @Valid @RequestBody FormDtos.UpsertRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Form updated successfully",
                        definitionService.update(tenantId, projectId, formId, request)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{formId}/activate")
    public ResponseEntity<ApiResponse<FormDtos.Response>> activate(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID formId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Form activated successfully",
                        definitionService.activate(tenantId, projectId, formId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{formId}/pause")
    public ResponseEntity<ApiResponse<FormDtos.Response>> pause(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID formId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Form paused successfully",
                        definitionService.pause(tenantId, projectId, formId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{formId}/submissions")
    public ResponseEntity<ApiResponse<FormDtos.SubmissionResponse>> submit(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID formId,
            @Valid @RequestBody FormDtos.SubmissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Form submitted successfully",
                        submissionService.submit(tenantId, projectId, formId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{formId}/submissions")
    public ResponseEntity<ApiResponse<PageResponse<FormDtos.SubmissionResponse>>> history(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID formId,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Form submission history fetched successfully",
                        submissionService.history(tenantId, projectId, formId, pageable)));
    }
}
