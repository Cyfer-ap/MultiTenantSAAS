package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.ProjectMember;
import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, UUID> {

    boolean existsByProject_IdAndUser_Id(
            UUID projectId,
            UUID userId
    );

    Optional<ProjectMember>
    findByProject_Tenant_IdAndProject_IdAndUser_Id(
            UUID tenantId,
            UUID projectId,
            UUID userId
    );

    long countByProject_IdAndRole(
            UUID projectId,
            ProjectMemberRole role
    );

    @Query("""
            SELECT membership
            FROM ProjectMember membership
            JOIN FETCH membership.user memberUser
            JOIN FETCH membership.assignedByUser assignedBy
            WHERE membership.project.tenant.id = :tenantId
              AND membership.project.id = :projectId
              AND (
                    :role IS NULL
                    OR membership.role = :role
              )
              AND (
                    :search IS NULL
                    OR LOWER(memberUser.fullName)
                        LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(memberUser.email)
                        LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<ProjectMember> findProjectMembers(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("role") ProjectMemberRole role,
            @Param("search") String search,
            Pageable pageable
    );
}