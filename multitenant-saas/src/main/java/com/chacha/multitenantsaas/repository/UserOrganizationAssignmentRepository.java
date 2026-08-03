package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserOrganizationAssignmentRepository
        extends JpaRepository<
        UserOrganizationAssignment,
        UUID
        > {

    Optional<UserOrganizationAssignment>
    findByTenant_IdAndId(
            UUID tenantId,
            UUID assignmentId
    );

    boolean
    existsByTenant_IdAndUser_IdAndPrimaryAssignmentTrueAndStatus(
            UUID tenantId,
            UUID userId,
            OrganizationAssignmentStatus status
    );

    @Query("""
            SELECT assignment
            FROM UserOrganizationAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.organizationalUnit unit
            JOIN FETCH assignment.createdByUser createdBy
            LEFT JOIN FETCH
                assignment.reportsToAssignment managerAssignment
            WHERE assignment.tenant.id = :tenantId
              AND assignment.user.id = :userId
            ORDER BY
                assignment.primaryAssignment DESC,
                assignment.status ASC,
                assignment.validFrom DESC,
                assignment.id ASC
            """)
    List<UserOrganizationAssignment>
    findUserAssignments(
            @Param("tenantId")
            UUID tenantId,

            @Param("userId")
            UUID userId
    );

    @Query("""
            SELECT assignment
            FROM UserOrganizationAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.organizationalUnit unit
            JOIN FETCH assignment.createdByUser createdBy
            LEFT JOIN FETCH
                assignment.reportsToAssignment managerAssignment
            WHERE assignment.tenant.id = :tenantId
              AND assignment.organizationalUnit.id =
                    :organizationalUnitId
            ORDER BY
                assignedUser.fullName ASC,
                assignment.primaryAssignment DESC,
                assignment.validFrom DESC,
                assignment.id ASC
            """)
    List<UserOrganizationAssignment>
    findUnitAssignments(
            @Param("tenantId")
            UUID tenantId,

            @Param("organizationalUnitId")
            UUID organizationalUnitId
    );

    @Query("""
            SELECT assignment
            FROM UserOrganizationAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.organizationalUnit unit
            JOIN FETCH assignment.createdByUser createdBy
            LEFT JOIN FETCH
                assignment.reportsToAssignment managerAssignment
            WHERE assignment.tenant.id = :tenantId
              AND assignment.reportsToAssignment.id =
                    :managerAssignmentId
            ORDER BY
                assignedUser.fullName ASC,
                assignment.id ASC
            """)
    List<UserOrganizationAssignment>
    findDirectReports(
            @Param("tenantId")
            UUID tenantId,

            @Param("managerAssignmentId")
            UUID managerAssignmentId
    );

    @Query("""
            SELECT assignment
            FROM UserOrganizationAssignment assignment
            JOIN FETCH assignment.user assignedUser
            JOIN FETCH assignment.organizationalUnit unit
            JOIN FETCH assignment.createdByUser createdBy
            LEFT JOIN FETCH
                assignment.reportsToAssignment managerAssignment
            WHERE assignment.tenant.id = :tenantId
              AND assignment.user.id = :userId
              AND assignment.status =
                    :activeStatus
              AND assignment.validFrom <= :effectiveAt
              AND (
                    assignment.validUntil IS NULL
                    OR assignment.validUntil > :effectiveAt
              )
            ORDER BY
                assignment.primaryAssignment DESC,
                assignment.validFrom DESC,
                assignment.id ASC
            """)
    List<UserOrganizationAssignment>
    findEffectiveAssignmentsForUser(
            @Param("tenantId")
            UUID tenantId,

            @Param("userId")
            UUID userId,

            @Param("activeStatus")
            OrganizationAssignmentStatus activeStatus,

            @Param("effectiveAt")
            Instant effectiveAt
    );
}