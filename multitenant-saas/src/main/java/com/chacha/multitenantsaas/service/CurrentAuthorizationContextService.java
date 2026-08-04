package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.CurrentAuthorizationContextResponse;
import com.chacha.multitenantsaas.dto.CurrentAuthorizationGrantResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRolePermission;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.repository.AuthorizationRolePermissionRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurrentAuthorizationContextService {

    private final CurrentActorService
            currentActorService;

    private final AuthorizationUserRoleAssignmentService
            assignmentService;

    private final AuthorizationRolePermissionRepository
            rolePermissionRepository;

    public CurrentAuthorizationContextService(
            CurrentActorService currentActorService,
            AuthorizationUserRoleAssignmentService
                    assignmentService,
            AuthorizationRolePermissionRepository
                    rolePermissionRepository
    ) {
        this.currentActorService =
                currentActorService;

        this.assignmentService =
                assignmentService;

        this.rolePermissionRepository =
                rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public CurrentAuthorizationContextResponse
    getCurrentAuthorizationContext(
            UUID tenantId,
            Jwt jwt
    ) {
        AppUser actor =
                currentActorService
                        .getRequiredActiveActor(
                                tenantId,
                                jwt
                        );

        Instant evaluatedAt =
                Instant.now()
                        .truncatedTo(
                                ChronoUnit.MICROS
                        );

        List<AuthorizationUserRoleAssignmentResponse>
                assignments =
                assignmentService
                        .getEffectiveUserAssignments(
                                tenantId,
                                actor.getId(),
                                evaluatedAt
                        );

        Set<UUID> roleIds =
                assignments
                        .stream()
                        .map(
                                AuthorizationUserRoleAssignmentResponse
                                        ::roleId
                        )
                        .collect(Collectors.toSet());

        Map<UUID, Set<String>>
                permissionCodesByRole =
                loadPermissionCodesByRole(
                        tenantId,
                        roleIds
                );

        List<CurrentAuthorizationGrantResponse>
                grants =
                assignments
                        .stream()
                        .map(
                                assignment ->
                                        mapGrant(
                                                assignment,
                                                permissionCodesByRole
                                        )
                        )
                        .toList();

        TreeSet<String> allPermissionCodes =
                new TreeSet<>();

        TreeSet<String> tenantPermissionCodes =
                new TreeSet<>();

        for (
                CurrentAuthorizationGrantResponse grant
                : grants
        ) {
            allPermissionCodes.addAll(
                    grant.permissionCodes()
            );

            if (grant.scopeType()
                    == AuthorizationScopeType.TENANT) {
                tenantPermissionCodes.addAll(
                        grant.permissionCodes()
                );
            }
        }

        return new CurrentAuthorizationContextResponse(
                tenantId,
                actor.getId(),
                actor.getFullName(),
                actor.getEmail(),
                evaluatedAt,
                List.copyOf(
                        tenantPermissionCodes
                ),
                List.copyOf(
                        allPermissionCodes
                ),
                grants
        );
    }

    private Map<UUID, Set<String>>
    loadPermissionCodesByRole(
            UUID tenantId,
            Set<UUID> roleIds
    ) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }

        List<AuthorizationRolePermission>
                mappings =
                rolePermissionRepository
                        .findActiveRolePermissions(
                                tenantId,
                                roleIds,
                                AuthorizationRoleStatus.ACTIVE,
                                AuthorizationPermissionStatus.ACTIVE,
                                AuthorizationPermissionSource.PLATFORM
                        );

        Map<UUID, Set<String>> result =
                new LinkedHashMap<>();

        for (
                AuthorizationRolePermission mapping
                : mappings
        ) {
            result.computeIfAbsent(
                            mapping
                                    .getRole()
                                    .getId(),
                            ignored ->
                                    new TreeSet<>()
                    )
                    .add(
                            mapping
                                    .getPermission()
                                    .getCode()
                    );
        }

        return result;
    }

    private CurrentAuthorizationGrantResponse
    mapGrant(
            AuthorizationUserRoleAssignmentResponse
                    assignment,
            Map<UUID, Set<String>>
                    permissionCodesByRole
    ) {
        Set<String> permissionCodes =
                permissionCodesByRole
                        .getOrDefault(
                                assignment.roleId(),
                                Set.of()
                        );

        return new CurrentAuthorizationGrantResponse(
                assignment.id(),
                assignment.roleId(),
                assignment.roleCode(),
                assignment.roleName(),
                assignment.roleSource(),
                assignment.scopeType(),
                assignment.scopeTargetId(),
                assignment.validFrom(),
                assignment.validUntil(),
                List.copyOf(permissionCodes)
        );
    }
}