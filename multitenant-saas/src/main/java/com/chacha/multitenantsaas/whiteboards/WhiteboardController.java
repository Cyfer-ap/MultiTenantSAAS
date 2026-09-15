package com.chacha.multitenantsaas.whiteboards;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/whiteboards")
public class WhiteboardController {

    private final WhiteboardService whiteboardService;

    public WhiteboardController(WhiteboardService whiteboardService) {
        this.whiteboardService = whiteboardService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WhiteboardDtos.SummaryResponse>>> list(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Whiteboards fetched successfully",
                        whiteboardService.list(tenantId, projectId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<WhiteboardDtos.Response>> get(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID boardId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Whiteboard fetched successfully",
                        whiteboardService.get(tenantId, projectId, boardId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<WhiteboardDtos.Response>> create(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody WhiteboardDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Whiteboard created successfully",
                        whiteboardService.create(tenantId, projectId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/{boardId}")
    public ResponseEntity<ApiResponse<WhiteboardDtos.Response>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID boardId,
            @Valid @RequestBody WhiteboardDtos.UpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Whiteboard updated successfully",
                        whiteboardService.update(tenantId, projectId, boardId, request)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @DeleteMapping("/{boardId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID boardId,
            @RequestParam long expectedVersion) {
        whiteboardService.delete(tenantId, projectId, boardId, expectedVersion);
        return ResponseEntity.ok(ApiResponse.success("Whiteboard deleted successfully", null));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{boardId}/nodes/{nodeKey}/convert-to-task")
    public ResponseEntity<ApiResponse<WhiteboardDtos.ConvertToTaskResponse>> convertToTask(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID boardId,
            @PathVariable String nodeKey,
            @Valid @RequestBody WhiteboardDtos.ConvertToTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Whiteboard node converted to task successfully",
                        whiteboardService.convertNodeToTask(
                                tenantId, projectId, boardId, nodeKey, request, jwt)));
    }
}
