package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRolePermissionUpdateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.dto.TenantPermissionCreateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthorizationManagementCommandService {

    private final AuthorizationPermissionService
            authorizationPermissionService;

    private final AuthorizationRoleService
            authorizationRoleService;

    private final AuthorizationUserRoleAssignmentService
            authorizationUserRoleAssignmentService;

    private final CurrentActorService currentActorService;

    private final AppUserRepository appUserRepository;

    private final AuditLogService auditLogService;

    public AuthorizationManagementCommandService(
            AuthorizationPermissionService
                    authorizationPermissionService,
            AuthorizationRoleService
                    authorizationRoleService,
            AuthorizationUserRoleAssignmentService
                    authorizationUserRoleAssignmentService,
            CurrentActorService currentActorService,
            AppUserRepository appUserRepository,
            AuditLogService auditLogService
    ) {
        this.authorizationPermissionService =
                authorizationPermissionService;

        this.authorizationRoleService =
                authorizationRoleService;

        this.authorizationUserRoleAssignmentService =
                authorizationUserRoleAssignmentService;

        this.currentActorService =
                currentActorService;

        this.appUserRepository =
                appUserRepository;

        this.auditLogService =
                auditLogService;
    }

    @Transactional
    public AuthorizationPermissionResponse
    createTenantPermission(
            UUID tenantId,
            TenantPermissionCreateRequest request,
            Jwt jwt
    ) {
        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        AuthorizationPermissionResponse permission =
                authorizationPermissionService
                        .createTenantPermission(
                                tenantId,
                                request
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_PERMISSION_CREATED,
                "Authorization permission created: "
                        + permission.id()
                        + "; code="
                        + permission.code()
                        + "; category="
                        + permission.category()
        );

        return permission;
    }

    @Transactional
    public AuthorizationPermissionResponse
    deactivateTenantPermission(
            UUID tenantId,
            UUID permissionId,
            Jwt jwt
    ) {
        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        AuthorizationPermissionResponse existing =
                authorizationPermissionService
                        .getPermission(
                                tenantId,
                                permissionId
                        );

        AuthorizationPermissionResponse permission =
                authorizationPermissionService
                        .deactivateTenantPermission(
                                tenantId,
                                permissionId
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_PERMISSION_DEACTIVATED,
                "Authorization permission deactivated: "
                        + permission.id()
                        + "; code="
                        + existing.code()
                        + "; status="
                        + existing.status()
                        + " -> "
                        + permission.status()
        );

        return permission;
    }

    @Transactional
    public List<AuthorizationRoleResponse>
    initializeDefaultRoles(
            UUID tenantId,
            Jwt jwt
    ) {
        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        List<AuthorizationRoleResponse> roles =
                authorizationRoleService
                        .initializeDefaultRoles(
                                tenantId
                        );

        String initializedCodes =
                roles.stream()
                        .map(
                                AuthorizationRoleResponse::code
                        )
                        .sorted()
                        .toList()
                        .toString();

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_ROLES_INITIALIZED,
                "System authorization roles initialized "
                        + "or synchronized: "
                        + initializedCodes
        );

        return roles;
    }

    @Transactional
    public AuthorizationRoleResponse createTenantRole(
            UUID tenantId,
            AuthorizationRoleCreateRequest request,
            Jwt jwt
    ) {
        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        AuthorizationRoleResponse role =
                authorizationRoleService
                        .createTenantRole(
                                tenantId,
                                request
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_ROLE_CREATED,
                "Authorization role created: "
                        + role.id()
                        + "; code="
                        + role.code()
                        + "; permissions="
                        + role.permissions().size()
        );

        return role;
    }

    @Transactional
    public AuthorizationRoleResponse
    replaceTenantRolePermissions(
            UUID tenantId,
            UUID roleId,
            AuthorizationRolePermissionUpdateRequest request,
            Jwt jwt
    ) {
        if (request == null
                || request.permissionIds() == null) {
            throw new IllegalArgumentException(
                    "Permission ids are required."
            );
        }

        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        AuthorizationRoleResponse previousRole =
                authorizationRoleService
                        .getRole(
                                tenantId,
                                roleId
                        );

        AuthorizationRoleResponse updatedRole =
                authorizationRoleService
                        .replaceTenantRolePermissions(
                                tenantId,
                                roleId,
                                request.permissionIds()
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_ROLE_PERMISSIONS_UPDATED,
                "Authorization role permissions updated: "
                        + updatedRole.id()
                        + "; code="
                        + updatedRole.code()
                        + "; permissionCount="
                        + previousRole.permissions().size()
                        + " -> "
                        + updatedRole.permissions().size()
        );

        return updatedRole;
    }

    @Transactional
    public AuthorizationRoleResponse deactivateTenantRole(
            UUID tenantId,
            UUID roleId,
            Jwt jwt
    ) {
        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        AuthorizationRoleResponse previousRole =
                authorizationRoleService
                        .getRole(
                                tenantId,
                                roleId
                        );

        AuthorizationRoleResponse role =
                authorizationRoleService
                        .deactivateTenantRole(
                                tenantId,
                                roleId
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.AUTH_ROLE_DEACTIVATED,
                "Authorization role deactivated: "
                        + role.id()
                        + "; code="
                        + role.code()
                        + "; status="
                        + previousRole.status()
                        + " -> "
                        + role.status()
        );

        return role;
    }

    @Transactional
    public AuthorizationUserRoleAssignmentResponse
    createUserRoleAssignment(
            UUID tenantId,
            AuthorizationUserRoleAssignmentCreateRequest request,
            Jwt jwt
    ) {
        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        AuthorizationUserRoleAssignmentResponse assignment =
                authorizationUserRoleAssignmentService
                        .createAssignment(
                                tenantId,
                                actor.getId(),
                                request
                        );

        AppUser targetUser =
                getTargetUser(
                        tenantId,
                        assignment.userId()
                );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                targetUser,
                AuditAction.AUTH_USER_ROLE_ASSIGNED,
                "Authorization role assigned: "
                        + assignment.id()
                        + "; userId="
                        + assignment.userId()
                        + "; roleId="
                        + assignment.roleId()
                        + "; roleCode="
                        + assignment.roleCode()
                        + "; scopeType="
                        + assignment.scopeType()
                        + "; scopeTargetId="
                        + formatNullableUuid(
                        assignment.scopeTargetId()
                )
        );

        return assignment;
    }

    @Transactional
    public AuthorizationUserRoleAssignmentResponse
    deactivateUserRoleAssignment(
            UUID tenantId,
            UUID assignmentId,
            Jwt jwt
    ) {
        AppUser actor =
                getActor(
                        tenantId,
                        jwt
                );

        AuthorizationUserRoleAssignmentResponse existing =
                authorizationUserRoleAssignmentService
                        .getAssignment(
                                tenantId,
                                assignmentId
                        );

        AppUser targetUser =
                getTargetUser(
                        tenantId,
                        existing.userId()
                );

        AuthorizationUserRoleAssignmentResponse assignment =
                authorizationUserRoleAssignmentService
                        .deactivateAssignment(
                                tenantId,
                                assignmentId
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                targetUser,
                AuditAction
                        .AUTH_USER_ROLE_ASSIGNMENT_DEACTIVATED,
                "Authorization role assignment "
                        + "deactivated: "
                        + assignment.id()
                        + "; userId="
                        + assignment.userId()
                        + "; roleCode="
                        + assignment.roleCode()
                        + "; scopeType="
                        + assignment.scopeType()
                        + "; status="
                        + existing.status()
                        + " -> "
                        + assignment.status()
        );

        return assignment;
    }

    private AppUser getActor(
            UUID tenantId,
            Jwt jwt
    ) {
        return currentActorService
                .getRequiredActiveActor(
                        tenantId,
                        jwt
                );
    }

    private AppUser getTargetUser(
            UUID tenantId,
            UUID userId
    ) {
        return appUserRepository
                .findByTenantIdAndId(
                        tenantId,
                        userId
                )
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Authorization target user "
                                                + "not found with id: "
                                                + userId
                                )
                );
    }

    private String formatNullableUuid(UUID value) {
        return value == null
                ? "NONE"
                : value.toString();
    }
}