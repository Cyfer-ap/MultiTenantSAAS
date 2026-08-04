package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignment;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationUserRoleAssignmentRepository
        extends JpaRepository<
        AuthorizationUserRoleAssignment,
        UUID
        > {

    Optional<AuthorizationUserRoleAssignment>
    findByTenant_IdAndId(
            UUID tenantId,
            UUID assignmentId
    );

    boolean existsByTenant_IdAndUser_Id(
            UUID tenantId,
            UUID userId
    );

    @Query("""
            SELECT assignment
            FROM AuthorizationUserRoleAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.role role
            JOIN FETCH assignment.createdByUser createdBy
            WHERE assignment.tenant.id = :tenantId
              AND assignedUser.id = :userId
            ORDER BY
                assignment.status ASC,
                role.code ASC,
                assignment.scopeType ASC,
                assignment.validFrom DESC,
                assignment.id ASC
            """)
    List<AuthorizationUserRoleAssignment>
    findUserAssignments(
            @Param("tenantId")
            UUID tenantId,

            @Param("userId")
            UUID userId
    );

    @Query("""
            SELECT assignment
            FROM AuthorizationUserRoleAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.role role
            JOIN FETCH assignment.createdByUser createdBy
            WHERE assignment.tenant.id = :tenantId
              AND role.id = :roleId
            ORDER BY
                assignedUser.fullName ASC,
                assignment.scopeType ASC,
                assignment.validFrom DESC,
                assignment.id ASC
            """)
    List<AuthorizationUserRoleAssignment>
    findRoleAssignments(
            @Param("tenantId")
            UUID tenantId,

            @Param("roleId")
            UUID roleId
    );

    @Query("""
            SELECT assignment
            FROM AuthorizationUserRoleAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.role role
            JOIN FETCH assignment.createdByUser createdBy
            WHERE assignment.tenant.id = :tenantId
              AND assignedUser.id = :userId
              AND assignment.status = :activeStatus
              AND role.status = :activeRoleStatus
              AND assignment.validFrom <= :effectiveAt
              AND (
                    assignment.validUntil IS NULL
                    OR assignment.validUntil > :effectiveAt
              )
            ORDER BY
                role.code ASC,
                assignment.scopeType ASC,
                assignment.id ASC
            """)
    List<AuthorizationUserRoleAssignment>
    findEffectiveAssignmentsForUser(
            @Param("tenantId")
            UUID tenantId,

            @Param("userId")
            UUID userId,

            @Param("activeStatus")
            AuthorizationUserRoleAssignmentStatus activeStatus,

            @Param("activeRoleStatus")
            AuthorizationRoleStatus activeRoleStatus,

            @Param("effectiveAt")
            Instant effectiveAt
    );

    @Query("""
            SELECT assignment
            FROM AuthorizationUserRoleAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.role role
            JOIN FETCH assignment.createdByUser createdBy
            WHERE assignment.tenant.id = :tenantId
              AND assignedUser.id = :userId
              AND assignment.status = :activeAssignmentStatus
              AND role.status = :activeRoleStatus
              AND assignment.validFrom <= :effectiveAt
              AND (
                    assignment.validUntil IS NULL
                    OR assignment.validUntil > :effectiveAt
              )
              AND EXISTS (
                    SELECT mapping.id
                    FROM AuthorizationRolePermission mapping
                    JOIN mapping.permission permission
                    WHERE mapping.tenant.id = :tenantId
                      AND mapping.role.id = role.id
                      AND permission.code = :permissionCode
                      AND permission.status =
                            :activePermissionStatus
                      AND (
                            permission.source =
                                :platformPermissionSource
                            OR permission.tenant.id =
                                :tenantId
                      )
              )
            ORDER BY
                role.code ASC,
                assignment.scopeType ASC,
                assignment.id ASC
            """)
    List<AuthorizationUserRoleAssignment>
    findEffectiveAssignmentsGrantingPermission(
            @Param("tenantId")
            UUID tenantId,

            @Param("userId")
            UUID userId,

            @Param("permissionCode")
            String permissionCode,

            @Param("activeAssignmentStatus")
            AuthorizationUserRoleAssignmentStatus
                    activeAssignmentStatus,

            @Param("activeRoleStatus")
            AuthorizationRoleStatus activeRoleStatus,

            @Param("activePermissionStatus")
            AuthorizationPermissionStatus
                    activePermissionStatus,

            @Param("platformPermissionSource")
            AuthorizationPermissionSource
                    platformPermissionSource,

            @Param("effectiveAt")
            Instant effectiveAt
    );

    @Query("""
            SELECT COUNT(assignment)
            FROM AuthorizationUserRoleAssignment assignment
            WHERE assignment.tenant.id = :tenantId
              AND assignment.user.id = :userId
              AND assignment.role.id = :roleId
              AND assignment.scopeType = :scopeType
              AND assignment.scopeKey = :scopeKey
              AND assignment.status = :activeStatus
              AND (
                    :validUntil IS NULL
                    OR assignment.validFrom < :validUntil
              )
              AND (
                    assignment.validUntil IS NULL
                    OR assignment.validUntil > :validFrom
              )
            """)
    long countOverlappingActiveAssignments(
            @Param("tenantId")
            UUID tenantId,

            @Param("userId")
            UUID userId,

            @Param("roleId")
            UUID roleId,

            @Param("scopeType")
            AuthorizationScopeType scopeType,

            @Param("scopeKey")
            String scopeKey,

            @Param("activeStatus")
            AuthorizationUserRoleAssignmentStatus
                    activeStatus,

            @Param("validFrom")
            Instant validFrom,

            @Param("validUntil")
            Instant validUntil
    );
}