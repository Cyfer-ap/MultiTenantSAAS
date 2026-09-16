package com.chacha.multitenantsaas.projectrisk;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.projectrisk.dto.ProjectRiskDtos.Response;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/risk")
public class ProjectRiskController {

    private final ProjectRiskService riskService;

    public ProjectRiskController(ProjectRiskService riskService) {
        this.riskService = riskService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<Response>> analyze(
            @PathVariable UUID tenantId, @PathVariable UUID projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project risk analysis loaded successfully",
                        riskService.analyze(tenantId, projectId)));
    }
}
