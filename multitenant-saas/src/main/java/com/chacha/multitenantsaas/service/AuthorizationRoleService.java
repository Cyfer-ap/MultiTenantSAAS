package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRole;
import com.chacha.multitenantsaas.entity.AuthorizationRolePermission;
import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRolePermissionRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRoleRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthorizationRoleService {

    private static final Pattern ROLE_CODE_PATTERN =
            Pattern.compile(
                    "^[A-Z][A-Z0-9_]{0,59}$"
            );

    private static final Set<String>
            MANAGER_PERMISSION_CODES =
            Set.of(
                    PlatformPermissionCodes.TENANT_READ,
                    PlatformPermissionCodes.USER_READ,
                    PlatformPermissionCodes
                            .ORGANIZATION_UNIT_READ,
                    PlatformPermissionCodes
                            .ORGANIZATION_ASSIGNMENT_READ,
                    PlatformPermissionCodes.PROJECT_READ,
                    PlatformPermissionCodes.PROJECT_CREATE,
                    PlatformPermissionCodes.PROJECT_UPDATE,
                    PlatformPermissionCodes.PROJECT_ARCHIVE,
                    PlatformPermissionCodes
                            .PROJECT_MEMBER_MANAGE,
                    PlatformPermissionCodes
                            .PROJECT_TASK_READ,
                    PlatformPermissionCodes
                            .PROJECT_TASK_MANAGE
            );

    private static final Set<String>
            MEMBER_PERMISSION_CODES =
            Set.of(
                    PlatformPermissionCodes.TENANT_READ,
                    PlatformPermissionCodes.USER_READ,
                    PlatformPermissionCodes
                            .ORGANIZATION_UNIT_READ,
                    PlatformPermissionCodes
                            .ORGANIZATION_ASSIGNMENT_READ,
                    PlatformPermissionCodes.PROJECT_READ
            );

    private final TenantLookupService tenantLookupService;

    private final AuthorizationRoleRepository
            authorizationRoleRepository;

    private final AuthorizationRolePermissionRepository
            authorizationRolePermissionRepository;

    private final AuthorizationPermissionRepository
            authorizationPermissionRepository;

    public AuthorizationRoleService(
            TenantLookupService tenantLookupService,
            AuthorizationRoleRepository
                    authorizationRoleRepository,
            AuthorizationRolePermissionRepository
                    authorizationRolePermissionRepository,
            AuthorizationPermissionRepository
                    authorizationPermissionRepository
    ) {
        this.tenantLookupService =
                tenantLookupService;

        this.authorizationRoleRepository =
                authorizationRoleRepository;

        this.authorizationRolePermissionRepository =
                authorizationRolePermissionRepository;

        this.authorizationPermissionRepository =
                authorizationPermissionRepository;
    }

    @Transactional
    public List<AuthorizationRoleResponse>
    initializeDefaultRoles(UUID tenantId) {
        Tenant tenant =
                tenantLookupService
                        .getActiveByIdOrThrow(
                                tenantId
                        );

        List<AuthorizationPermission>
                platformPermissions =
                authorizationPermissionRepository
                        .findBySourceAndStatusOrderByCategoryAscCodeAsc(
                                AuthorizationPermissionSource.PLATFORM,
                                AuthorizationPermissionStatus.ACTIVE
                        );

        if (platformPermissions.isEmpty()) {
            throw new IllegalStateException(
                    "Platform permission catalog "
                            + "has not been initialized."
            );
        }

        Map<String, AuthorizationPermission>
                permissionsByCode =
                indexPermissionsByCode(
                        platformPermissions
                );

        AuthorizationRole adminRole =
                createOrSynchronizeSystemRole(
                        tenant,
                        SystemRoleCodes.ADMIN,
                        "Administrator",
                        "Full tenant administration access.",
                        platformPermissions
                );

        AuthorizationRole managerRole =
                createOrSynchronizeSystemRole(
                        tenant,
                        SystemRoleCodes.MANAGER,
                        "Manager",
                        "Operational management access "
                                + "without authorization "
                                + "administration.",
                        resolvePermissionsByCode(
                                permissionsByCode,
                                MANAGER_PERMISSION_CODES
                        )
                );

        AuthorizationRole memberRole =
                createOrSynchronizeSystemRole(
                        tenant,
                        SystemRoleCodes.MEMBER,
                        "Member",
                        "Standard tenant member access.",
                        resolvePermissionsByCode(
                                permissionsByCode,
                                MEMBER_PERMISSION_CODES
                        )
                );

        return List.of(
                mapToResponse(adminRole),
                mapToResponse(managerRole),
                mapToResponse(memberRole)
        );
    }

    @Transactional
    public AuthorizationRoleResponse createTenantRole(
            UUID tenantId,
            AuthorizationRoleCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Authorization role request "
                            + "is required."
            );
        }

        Tenant tenant =
                tenantLookupService
                        .getActiveByIdOrThrow(
                                tenantId
                        );

        String code =
                normalizeRoleCode(
                        request.code()
                );

        if (SystemRoleCodes.ALL.contains(code)) {
            throw new IllegalArgumentException(
                    "Role code is reserved for "
                            + "a system-managed role: "
                            + code
            );
        }

        String name =
                normalizeRequiredText(
                        request.name(),
                        "Role name",
                        150
                );

        String description =
                normalizeOptionalText(
                        request.description(),
                        "Role description",
                        500
                );

        if (request.permissionIds() == null) {
            throw new IllegalArgumentException(
                    "Permission ids are required."
            );
        }

        if (authorizationRoleRepository
                .existsByTenant_IdAndCode(
                        tenantId,
                        code
                )) {
            throw new DuplicateResourceException(
                    "Authorization role already exists "
                            + "with code: "
                            + code
            );
        }

        List<AuthorizationPermission> permissions =
                resolveAccessiblePermissions(
                        tenantId,
                        request.permissionIds()
                );

        AuthorizationRole role =
                new AuthorizationRole(
                        tenant,
                        code,
                        name,
                        description,
                        AuthorizationRoleSource.TENANT
                );

        AuthorizationRole savedRole =
                authorizationRoleRepository
                        .saveAndFlush(role);

        replacePermissionMappings(
                tenant,
                savedRole,
                permissions
        );

        return mapToResponse(savedRole);
    }

    @Transactional(readOnly = true)
    public AuthorizationRoleResponse getRole(
            UUID tenantId,
            UUID roleId
    ) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        return mapToResponse(
                getRoleOrThrow(
                        tenantId,
                        roleId
                )
        );
    }

    @Transactional(readOnly = true)
    public AuthorizationRoleResponse getRoleByCode(
            UUID tenantId,
            String roleCode
    ) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        String normalizedCode =
                normalizeRoleCode(roleCode);

        AuthorizationRole role =
                authorizationRoleRepository
                        .findByTenant_IdAndCode(
                                tenantId,
                                normalizedCode
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Authorization role "
                                                        + "not found "
                                                        + "with code: "
                                                        + normalizedCode
                                        )
                        );

        return mapToResponse(role);
    }

    @Transactional(readOnly = true)
    public List<AuthorizationRoleResponse>
    getRoles(UUID tenantId) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        return authorizationRoleRepository
                .findByTenant_IdOrderBySourceAscCodeAsc(
                        tenantId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorizationRoleResponse>
    getActiveRoles(UUID tenantId) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        return authorizationRoleRepository
                .findByTenant_IdAndStatusOrderBySourceAscCodeAsc(
                        tenantId,
                        AuthorizationRoleStatus.ACTIVE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public AuthorizationRoleResponse
    replaceTenantRolePermissions(
            UUID tenantId,
            UUID roleId,
            Set<UUID> permissionIds
    ) {
        Tenant tenant =
                tenantLookupService
                        .getActiveByIdOrThrow(
                                tenantId
                        );

        AuthorizationRole role =
                getRoleOrThrow(
                        tenantId,
                        roleId
                );

        requireTenantManagedRole(role);

        if (permissionIds == null) {
            throw new IllegalArgumentException(
                    "Permission ids are required."
            );
        }

        List<AuthorizationPermission> permissions =
                resolveAccessiblePermissions(
                        tenantId,
                        permissionIds
                );

        replacePermissionMappings(
                tenant,
                role,
                permissions
        );

        return mapToResponse(role);
    }

    @Transactional
    public AuthorizationRoleResponse
    deactivateTenantRole(
            UUID tenantId,
            UUID roleId
    ) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        AuthorizationRole role =
                getRoleOrThrow(
                        tenantId,
                        roleId
                );

        requireTenantManagedRole(role);

        if (role.getStatus()
                == AuthorizationRoleStatus.INACTIVE) {
            return mapToResponse(role);
        }

        role.setStatus(
                AuthorizationRoleStatus.INACTIVE
        );

        AuthorizationRole savedRole =
                authorizationRoleRepository
                        .saveAndFlush(role);

        return mapToResponse(savedRole);
    }

    private AuthorizationRole
    createOrSynchronizeSystemRole(
            Tenant tenant,
            String code,
            String name,
            String description,
            List<AuthorizationPermission> permissions
    ) {
        AuthorizationRole role =
                authorizationRoleRepository
                        .findByTenant_IdAndCode(
                                tenant.getId(),
                                code
                        )
                        .map(
                                existingRole -> {
                                    if (existingRole.getSource()
                                            != AuthorizationRoleSource.SYSTEM) {
                                        throw new IllegalStateException(
                                                "Reserved system role "
                                                        + "code is already "
                                                        + "used by a tenant "
                                                        + "role: "
                                                        + code
                                        );
                                    }

                                    existingRole.setName(name);
                                    existingRole.setDescription(
                                            description
                                    );
                                    existingRole.setStatus(
                                            AuthorizationRoleStatus.ACTIVE
                                    );

                                    return existingRole;
                                }
                        )
                        .orElseGet(
                                () ->
                                        new AuthorizationRole(
                                                tenant,
                                                code,
                                                name,
                                                description,
                                                AuthorizationRoleSource.SYSTEM
                                        )
                        );

        AuthorizationRole savedRole =
                authorizationRoleRepository
                        .saveAndFlush(role);

        replacePermissionMappings(
                tenant,
                savedRole,
                permissions
        );

        return savedRole;
    }

    private void replacePermissionMappings(
            Tenant tenant,
            AuthorizationRole role,
            List<AuthorizationPermission> permissions
    ) {
        authorizationRolePermissionRepository
                .deleteByTenant_IdAndRole_Id(
                        tenant.getId(),
                        role.getId()
                );

        authorizationRolePermissionRepository.flush();

        if (permissions.isEmpty()) {
            return;
        }

        List<AuthorizationRolePermission> mappings =
                permissions
                        .stream()
                        .map(
                                permission ->
                                        new AuthorizationRolePermission(
                                                tenant,
                                                role,
                                                permission
                                        )
                        )
                        .toList();

        authorizationRolePermissionRepository
                .saveAll(mappings);

        authorizationRolePermissionRepository.flush();
    }

    private List<AuthorizationPermission>
    resolveAccessiblePermissions(
            UUID tenantId,
            Set<UUID> permissionIds
    ) {
        if (permissionIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, AuthorizationPermission>
                uniquePermissions =
                new LinkedHashMap<>();

        for (UUID permissionId : permissionIds) {
            if (permissionId == null) {
                throw new IllegalArgumentException(
                        "Permission id must not be null."
                );
            }

            AuthorizationPermission permission =
                    authorizationPermissionRepository
                            .findAccessiblePermissionById(
                                    tenantId,
                                    permissionId,
                                    AuthorizationPermissionSource.PLATFORM
                            )
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Permission not found "
                                                            + "with id: "
                                                            + permissionId
                                            )
                            );

            if (permission.getStatus()
                    != AuthorizationPermissionStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Inactive permission cannot "
                                + "be assigned to a role: "
                                + permission.getCode()
                );
            }

            if (permission.getSource()
                    == AuthorizationPermissionSource.TENANT) {
                if (permission.getTenant() == null
                        || !permission
                        .getTenant()
                        .getId()
                        .equals(tenantId)) {
                    throw new ResourceNotFoundException(
                            "Permission not found "
                                    + "with id: "
                                    + permissionId
                    );
                }
            }

            uniquePermissions.put(
                    permission.getId(),
                    permission
            );
        }

        return new ArrayList<>(
                uniquePermissions.values()
        );
    }

    private Map<String, AuthorizationPermission>
    indexPermissionsByCode(
            List<AuthorizationPermission> permissions
    ) {
        Map<String, AuthorizationPermission> result =
                new LinkedHashMap<>();

        for (AuthorizationPermission permission
                : permissions) {
            result.put(
                    permission.getCode(),
                    permission
            );
        }

        return result;
    }

    private List<AuthorizationPermission>
    resolvePermissionsByCode(
            Map<String, AuthorizationPermission>
                    permissionsByCode,
            Set<String> permissionCodes
    ) {
        List<AuthorizationPermission> resolved =
                new ArrayList<>();

        for (String permissionCode
                : new LinkedHashSet<>(permissionCodes)) {
            AuthorizationPermission permission =
                    permissionsByCode.get(
                            permissionCode
                    );

            if (permission == null) {
                throw new IllegalStateException(
                        "Required platform permission "
                                + "is missing: "
                                + permissionCode
                );
            }

            resolved.add(permission);
        }

        return resolved;
    }

    private AuthorizationRole getRoleOrThrow(
            UUID tenantId,
            UUID roleId
    ) {
        if (roleId == null) {
            throw new IllegalArgumentException(
                    "Authorization role id is required."
            );
        }

        return authorizationRoleRepository
                .findByTenant_IdAndId(
                        tenantId,
                        roleId
                )
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Authorization role "
                                                + "not found "
                                                + "with id: "
                                                + roleId
                                )
                );
    }

    private void requireTenantManagedRole(
            AuthorizationRole role
    ) {
        if (role.getSource()
                != AuthorizationRoleSource.TENANT) {
            throw new IllegalArgumentException(
                    "System-managed authorization roles "
                            + "cannot be modified or "
                            + "deactivated."
            );
        }
    }

    private String normalizeRoleCode(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Role code is required."
            );
        }

        String normalized =
                value.trim()
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');

        if (!ROLE_CODE_PATTERN
                .matcher(normalized)
                .matches()) {
            throw new IllegalArgumentException(
                    "Role code must contain uppercase "
                            + "letters, digits, or "
                            + "underscores and must begin "
                            + "with a letter."
            );
        }

        return normalized;
    }

    private String normalizeRequiredText(
            String value,
            String fieldName,
            int maximumLength
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters."
            );
        }

        return normalized;
    }

    private String normalizeOptionalText(
            String value,
            String fieldName,
            int maximumLength
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters."
            );
        }

        return normalized;
    }

    private AuthorizationRoleResponse mapToResponse(
            AuthorizationRole role
    ) {
        List<AuthorizationPermissionResponse>
                permissionResponses =
                authorizationRolePermissionRepository
                        .findRolePermissions(
                                role.getTenant().getId(),
                                role.getId()
                        )
                        .stream()
                        .map(
                                mapping ->
                                        mapPermissionToResponse(
                                                mapping.getPermission()
                                        )
                        )
                        .toList();

        return new AuthorizationRoleResponse(
                role.getId(),
                role.getTenant().getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getSource(),
                role.getStatus(),
                permissionResponses,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private AuthorizationPermissionResponse
    mapPermissionToResponse(
            AuthorizationPermission permission
    ) {
        UUID permissionTenantId =
                permission.getTenant() == null
                        ? null
                        : permission
                        .getTenant()
                        .getId();

        return new AuthorizationPermissionResponse(
                permission.getId(),
                permissionTenantId,
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.getCategory(),
                permission.getSource(),
                permission.getStatus(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}