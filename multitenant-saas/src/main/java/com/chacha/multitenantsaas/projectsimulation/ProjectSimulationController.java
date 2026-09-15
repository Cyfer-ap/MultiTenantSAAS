package com.chacha.multitenantsaas.projectsimulation;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.BaselineResponse;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.Request;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.Response;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/simulation")
public class ProjectSimulationController {

    private final ProjectSimulationService simulationService;

    public ProjectSimulationController(ProjectSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @GetMapping("/baseline")
    public ResponseEntity<ApiResponse<BaselineResponse>> baseline(
            @PathVariable UUID tenantId, @PathVariable UUID projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project simulation baseline loaded successfully",
                        simulationService.baseline(tenantId, projectId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<Response>> simulate(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody Request request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project simulation calculated successfully",
                        simulationService.simulate(tenantId, projectId, request)));
    }
}
