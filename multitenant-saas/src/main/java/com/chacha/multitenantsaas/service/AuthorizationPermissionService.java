package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.TenantPermissionCreateRequest;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthorizationPermissionService {

    private static final Pattern
            CUSTOM_PERMISSION_CODE_PATTERN =
            Pattern.compile(
                    "^custom\\.[a-z][a-z0-9]*"
                            + "(?:\\.[a-z][a-z0-9]*)*$"
            );

    private static final Pattern CATEGORY_PATTERN =
            Pattern.compile(
                    "^[A-Z][A-Z0-9_]{0,59}$"
            );

    private final TenantLookupService tenantLookupService;

    private final AuthorizationPermissionRepository
            authorizationPermissionRepository;

    public AuthorizationPermissionService(
            TenantLookupService tenantLookupService,
            AuthorizationPermissionRepository
                    authorizationPermissionRepository
    ) {
        this.tenantLookupService =
                tenantLookupService;

        this.authorizationPermissionRepository =
                authorizationPermissionRepository;
    }

    @Transactional
    public AuthorizationPermissionResponse
    createTenantPermission(
            UUID tenantId,
            TenantPermissionCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Tenant permission request is required."
            );
        }

        Tenant tenant =
                tenantLookupService
                        .getActiveByIdOrThrow(
                                tenantId
                        );

        String code =
                normalizeCustomPermissionCode(
                        request.code()
                );

        String name =
                normalizeRequiredText(
                        request.name(),
                        "Permission name",
                        150
                );

        String description =
                normalizeOptionalText(
                        request.description(),
                        "Permission description",
                        500
                );

        String category =
                normalizeCategory(
                        request.category()
                );

        authorizationPermissionRepository
                .findByTenant_IdAndCode(
                        tenantId,
                        code
                )
                .ifPresent(
                        existing ->
                                throwDuplicatePermission(
                                        code
                                )
                );

        authorizationPermissionRepository
                .findBySourceAndCode(
                        AuthorizationPermissionSource.PLATFORM,
                        code
                )
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Permission code conflicts "
                                    + "with a platform "
                                    + "permission: "
                                    + code
                    );
                });

        AuthorizationPermission permission =
                new AuthorizationPermission(
                        tenant,
                        code,
                        name,
                        description,
                        category
                );

        AuthorizationPermission savedPermission =
                authorizationPermissionRepository
                        .saveAndFlush(permission);

        return mapToResponse(savedPermission);
    }

    @Transactional(readOnly = true)
    public List<AuthorizationPermissionResponse>
    getAvailablePermissions(UUID tenantId) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        return authorizationPermissionRepository
                .findAvailablePermissions(
                        tenantId,
                        AuthorizationPermissionSource.PLATFORM,
                        AuthorizationPermissionStatus.ACTIVE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorizationPermissionResponse>
    getTenantPermissions(UUID tenantId) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        return authorizationPermissionRepository
                .findByTenant_IdOrderByCategoryAscCodeAsc(
                        tenantId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuthorizationPermissionResponse
    getPermission(
            UUID tenantId,
            UUID permissionId
    ) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        return mapToResponse(
                getAccessiblePermissionOrThrow(
                        tenantId,
                        permissionId
                )
        );
    }

    @Transactional(readOnly = true)
    public AuthorizationPermissionResponse
    getPermissionByCode(
            UUID tenantId,
            String permissionCode
    ) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        String normalizedCode =
                normalizeGeneralPermissionCode(
                        permissionCode
                );

        List<AuthorizationPermission> matches =
                authorizationPermissionRepository
                        .findAccessiblePermissionsByCode(
                                tenantId,
                                normalizedCode,
                                AuthorizationPermissionSource.PLATFORM,
                                AuthorizationPermissionStatus.ACTIVE
                        );

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Active permission not found "
                            + "with code: "
                            + normalizedCode
            );
        }

        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Permission code is ambiguous: "
                            + normalizedCode
            );
        }

        return mapToResponse(
                matches.getFirst()
        );
    }

    @Transactional
    public AuthorizationPermissionResponse
    deactivateTenantPermission(
            UUID tenantId,
            UUID permissionId
    ) {
        tenantLookupService
                .getActiveByIdOrThrow(tenantId);

        AuthorizationPermission permission =
                authorizationPermissionRepository
                        .findByTenant_IdAndId(
                                tenantId,
                                permissionId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant permission "
                                                        + "not found "
                                                        + "with id: "
                                                        + permissionId
                                        )
                        );

        if (permission.getStatus()
                == AuthorizationPermissionStatus.INACTIVE) {
            return mapToResponse(permission);
        }

        permission.setStatus(
                AuthorizationPermissionStatus.INACTIVE
        );

        AuthorizationPermission savedPermission =
                authorizationPermissionRepository
                        .saveAndFlush(permission);

        return mapToResponse(savedPermission);
    }

    private AuthorizationPermission
    getAccessiblePermissionOrThrow(
            UUID tenantId,
            UUID permissionId
    ) {
        if (permissionId == null) {
            throw new IllegalArgumentException(
                    "Permission id is required."
            );
        }

        return authorizationPermissionRepository
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
    }

    private String normalizeCustomPermissionCode(
            String value
    ) {
        String normalizedCode =
                normalizeGeneralPermissionCode(value);

        if (!CUSTOM_PERMISSION_CODE_PATTERN
                .matcher(normalizedCode)
                .matches()) {
            throw new IllegalArgumentException(
                    "Tenant permission code must begin "
                            + "with 'custom.' and contain "
                            + "lowercase dot-separated "
                            + "segments."
            );
        }

        return normalizedCode;
    }

    private String normalizeGeneralPermissionCode(
            String value
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Permission code is required."
            );
        }

        String normalizedCode =
                value.trim()
                        .toLowerCase(Locale.ROOT);

        if (normalizedCode.isEmpty()) {
            throw new IllegalArgumentException(
                    "Permission code is required."
            );
        }

        if (normalizedCode.length() > 120) {
            throw new IllegalArgumentException(
                    "Permission code must not exceed "
                            + "120 characters."
            );
        }

        return normalizedCode;
    }

    private String normalizeCategory(String value) {
        String category =
                normalizeRequiredText(
                        value,
                        "Permission category",
                        60
                )
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');

        if (!CATEGORY_PATTERN
                .matcher(category)
                .matches()) {
            throw new IllegalArgumentException(
                    "Permission category must contain "
                            + "uppercase letters, digits, "
                            + "or underscores."
            );
        }

        return category;
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

    private void throwDuplicatePermission(
            String permissionCode
    ) {
        throw new DuplicateResourceException(
                "Tenant permission already exists "
                        + "with code: "
                        + permissionCode
        );
    }

    private AuthorizationPermissionResponse
    mapToResponse(
            AuthorizationPermission permission
    ) {
        UUID tenantId =
                permission.getTenant() == null
                        ? null
                        : permission
                        .getTenant()
                        .getId();

        return new AuthorizationPermissionResponse(
                permission.getId(),
                tenantId,
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