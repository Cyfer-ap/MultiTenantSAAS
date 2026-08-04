package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRolePermissionUpdateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.dto.TenantPermissionCreateRequest;
import com.chacha.multitenantsaas.service.AuthorizationManagementCommandService;
import com.chacha.multitenantsaas.service.AuthorizationPermissionService;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/tenants/{tenantId}/authorization"
)
public class AuthorizationManagementController {

    private final AuthorizationPermissionService
            authorizationPermissionService;

    private final AuthorizationRoleService
            authorizationRoleService;

    private final AuthorizationUserRoleAssignmentService
            authorizationUserRoleAssignmentService;

    private final AuthorizationManagementCommandService
            authorizationManagementCommandService;

    public AuthorizationManagementController(
            AuthorizationPermissionService
                    authorizationPermissionService,
            AuthorizationRoleService
                    authorizationRoleService,
            AuthorizationUserRoleAssignmentService
                    authorizationUserRoleAssignmentService,
            AuthorizationManagementCommandService
                    authorizationManagementCommandService
    ) {
        this.authorizationPermissionService =
                authorizationPermissionService;

        this.authorizationRoleService =
                authorizationRoleService;

        this.authorizationUserRoleAssignmentService =
                authorizationUserRoleAssignmentService;

        this.authorizationManagementCommandService =
                authorizationManagementCommandService;
    }

    /*
     * Permission catalog
     */

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/permissions")
    public ResponseEntity<
            ApiResponse<
                    List<AuthorizationPermissionResponse>
                    >
            >
    getAvailablePermissions(
            @PathVariable UUID tenantId
    ) {
        List<AuthorizationPermissionResponse> response =
                authorizationPermissionService
                        .getAvailablePermissions(
                                tenantId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization permission catalog "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/permissions/custom")
    public ResponseEntity<
            ApiResponse<
                    List<AuthorizationPermissionResponse>
                    >
            >
    getTenantPermissions(
            @PathVariable UUID tenantId
    ) {
        List<AuthorizationPermissionResponse> response =
                authorizationPermissionService
                        .getTenantPermissions(
                                tenantId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant authorization permissions "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/permissions/{permissionId}")
    public ResponseEntity<
            ApiResponse<AuthorizationPermissionResponse>
            >
    getPermission(
            @PathVariable UUID tenantId,
            @PathVariable UUID permissionId
    ) {
        AuthorizationPermissionResponse response =
                authorizationPermissionService
                        .getPermission(
                                tenantId,
                                permissionId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization permission fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/permissions/by-code")
    public ResponseEntity<
            ApiResponse<AuthorizationPermissionResponse>
            >
    getPermissionByCode(
            @PathVariable UUID tenantId,
            @RequestParam String code
    ) {
        AuthorizationPermissionResponse response =
                authorizationPermissionService
                        .getPermissionByCode(
                                tenantId,
                                code
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization permission fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PostMapping("/permissions/custom")
    public ResponseEntity<
            ApiResponse<AuthorizationPermissionResponse>
            >
    createTenantPermission(
            @PathVariable UUID tenantId,
            @Valid @RequestBody
            TenantPermissionCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthorizationPermissionResponse response =
                authorizationManagementCommandService
                        .createTenantPermission(
                                tenantId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant authorization permission "
                                + "created successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PatchMapping(
            "/permissions/custom/{permissionId}"
                    + "/deactivate"
    )
    public ResponseEntity<
            ApiResponse<AuthorizationPermissionResponse>
            >
    deactivateTenantPermission(
            @PathVariable UUID tenantId,
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthorizationPermissionResponse response =
                authorizationManagementCommandService
                        .deactivateTenantPermission(
                                tenantId,
                                permissionId,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tenant authorization permission "
                                + "deactivated successfully",
                        response
                )
        );
    }

    /*
     * Roles
     */

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PostMapping("/roles/defaults/initialize")
    public ResponseEntity<
            ApiResponse<
                    List<AuthorizationRoleResponse>
                    >
            >
    initializeDefaultRoles(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<AuthorizationRoleResponse> response =
                authorizationManagementCommandService
                        .initializeDefaultRoles(
                                tenantId,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Default authorization roles "
                                + "initialized successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/roles")
    public ResponseEntity<
            ApiResponse<
                    List<AuthorizationRoleResponse>
                    >
            >
    getRoles(
            @PathVariable UUID tenantId,
            @RequestParam(
                    defaultValue = "false"
            )
            boolean activeOnly
    ) {
        List<AuthorizationRoleResponse> response =
                activeOnly
                        ? authorizationRoleService
                        .getActiveRoles(tenantId)
                        : authorizationRoleService
                        .getRoles(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization roles fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/roles/{roleId}")
    public ResponseEntity<
            ApiResponse<AuthorizationRoleResponse>
            >
    getRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID roleId
    ) {
        AuthorizationRoleResponse response =
                authorizationRoleService
                        .getRole(
                                tenantId,
                                roleId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/roles/by-code")
    public ResponseEntity<
            ApiResponse<AuthorizationRoleResponse>
            >
    getRoleByCode(
            @PathVariable UUID tenantId,
            @RequestParam String code
    ) {
        AuthorizationRoleResponse response =
                authorizationRoleService
                        .getRoleByCode(
                                tenantId,
                                code
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PostMapping("/roles")
    public ResponseEntity<
            ApiResponse<AuthorizationRoleResponse>
            >
    createTenantRole(
            @PathVariable UUID tenantId,
            @Valid @RequestBody
            AuthorizationRoleCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthorizationRoleResponse response =
                authorizationManagementCommandService
                        .createTenantRole(
                                tenantId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role created "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PutMapping("/roles/{roleId}/permissions")
    public ResponseEntity<
            ApiResponse<AuthorizationRoleResponse>
            >
    replaceTenantRolePermissions(
            @PathVariable UUID tenantId,
            @PathVariable UUID roleId,
            @Valid @RequestBody
            AuthorizationRolePermissionUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthorizationRoleResponse response =
                authorizationManagementCommandService
                        .replaceTenantRolePermissions(
                                tenantId,
                                roleId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role permissions "
                                + "updated successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PatchMapping("/roles/{roleId}/deactivate")
    public ResponseEntity<
            ApiResponse<AuthorizationRoleResponse>
            >
    deactivateTenantRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID roleId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthorizationRoleResponse response =
                authorizationManagementCommandService
                        .deactivateTenantRole(
                                tenantId,
                                roleId,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role deactivated "
                                + "successfully",
                        response
                )
        );
    }

    /*
     * Scoped user-role assignments
     */

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PostMapping("/assignments")
    public ResponseEntity<
            ApiResponse<
                    AuthorizationUserRoleAssignmentResponse
                    >
            >
    createUserRoleAssignment(
            @PathVariable UUID tenantId,
            @Valid @RequestBody
            AuthorizationUserRoleAssignmentCreateRequest
                    request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthorizationUserRoleAssignmentResponse response =
                authorizationManagementCommandService
                        .createUserRoleAssignment(
                                tenantId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role assigned "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<
            ApiResponse<
                    AuthorizationUserRoleAssignmentResponse
                    >
            >
    getUserRoleAssignment(
            @PathVariable UUID tenantId,
            @PathVariable UUID assignmentId
    ) {
        AuthorizationUserRoleAssignmentResponse response =
                authorizationUserRoleAssignmentService
                        .getAssignment(
                                tenantId,
                                assignmentId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role assignment "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/assignments/users/{userId}")
    public ResponseEntity<
            ApiResponse<
                    List<
                            AuthorizationUserRoleAssignmentResponse
                            >
                    >
            >
    getUserRoleAssignments(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId
    ) {
        List<AuthorizationUserRoleAssignmentResponse>
                response =
                authorizationUserRoleAssignmentService
                        .getUserAssignments(
                                tenantId,
                                userId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User authorization role "
                                + "assignments fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping(
            "/assignments/users/{userId}/effective"
    )
    public ResponseEntity<
            ApiResponse<
                    List<
                            AuthorizationUserRoleAssignmentResponse
                            >
                    >
            >
    getEffectiveUserRoleAssignments(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant effectiveAt
    ) {
        List<AuthorizationUserRoleAssignmentResponse>
                response =
                authorizationUserRoleAssignmentService
                        .getEffectiveUserAssignments(
                                tenantId,
                                userId,
                                effectiveAt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Effective authorization role "
                                + "assignments fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @GetMapping("/assignments/roles/{roleId}")
    public ResponseEntity<
            ApiResponse<
                    List<
                            AuthorizationUserRoleAssignmentResponse
                            >
                    >
            >
    getRoleAssignments(
            @PathVariable UUID tenantId,
            @PathVariable UUID roleId
    ) {
        List<AuthorizationUserRoleAssignmentResponse>
                response =
                authorizationUserRoleAssignmentService
                        .getRoleAssignments(
                                tenantId,
                                roleId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role assignments "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")"
    )
    @PatchMapping(
            "/assignments/{assignmentId}/deactivate"
    )
    public ResponseEntity<
            ApiResponse<
                    AuthorizationUserRoleAssignmentResponse
                    >
            >
    deactivateUserRoleAssignment(
            @PathVariable UUID tenantId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthorizationUserRoleAssignmentResponse response =
                authorizationManagementCommandService
                        .deactivateUserRoleAssignment(
                                tenantId,
                                assignmentId,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authorization role assignment "
                                + "deactivated successfully",
                        response
                )
        );
    }
}
