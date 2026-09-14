package com.chacha.multitenantsaas.projecttemplates;

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
@RequestMapping("/api/tenants/{tenantId}/project-templates")
public class ProjectTemplateController {

    private final ProjectTemplateService projectTemplateService;

    public ProjectTemplateController(ProjectTemplateService projectTemplateService) {
        this.projectTemplateService = projectTemplateService;
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProjectTemplateDtos.Response>>> list(
            @PathVariable UUID tenantId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project templates fetched successfully",
                        projectTemplateService.list(tenantId, pageable)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.read')")
    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponse<ProjectTemplateDtos.Response>> get(
            @PathVariable UUID tenantId, @PathVariable UUID templateId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project template fetched successfully",
                        projectTemplateService.get(tenantId, templateId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.create')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectTemplateDtos.Response>> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ProjectTemplateDtos.UpsertRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project template created successfully",
                        projectTemplateService.create(tenantId, request, jwt)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.create')")
    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponse<ProjectTemplateDtos.Response>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID templateId,
            @Valid @RequestBody ProjectTemplateDtos.UpsertRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project template updated successfully",
                        projectTemplateService.update(tenantId, templateId, request)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.create')")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID tenantId, @PathVariable UUID templateId) {
        projectTemplateService.delete(tenantId, templateId);
        return ResponseEntity.ok(ApiResponse.success("Project template deleted successfully", null));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.create')")
    @PostMapping("/{templateId}/instantiate")
    public ResponseEntity<ApiResponse<ProjectTemplateDtos.InstantiateResponse>> instantiate(
            @PathVariable UUID tenantId,
            @PathVariable UUID templateId,
            @Valid @RequestBody(required = false) ProjectTemplateDtos.InstantiateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project created from template successfully",
                        projectTemplateService.instantiate(tenantId, templateId, request, jwt)));
    }
}
