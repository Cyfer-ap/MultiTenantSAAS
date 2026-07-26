package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectTaskRepository
        extends JpaRepository<ProjectTask, UUID> {

    Optional<ProjectTask>
    findByProject_Tenant_IdAndProject_IdAndId(
            UUID tenantId,
            UUID projectId,
            UUID taskId
    );

    @Query("""
            SELECT task
            FROM ProjectTask task
            WHERE task.tenant.id = :tenantId
              AND task.project.id = :projectId
              AND (
                    :status IS NULL
                    OR task.status = :status
              )
              AND (
                    :priority IS NULL
                    OR task.priority = :priority
              )
              AND (
                    :assigneeUserId IS NULL
                    OR task.assigneeUser.id = :assigneeUserId
              )
              AND (
                    :search IS NULL
                    OR LOWER(task.title)
                        LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(task.description, ''))
                        LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<ProjectTask> findProjectTasks(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("status") ProjectTaskStatus status,
            @Param("priority") ProjectTaskPriority priority,
            @Param("assigneeUserId") UUID assigneeUserId,
            @Param("search") String search,
            Pageable pageable
    );
}