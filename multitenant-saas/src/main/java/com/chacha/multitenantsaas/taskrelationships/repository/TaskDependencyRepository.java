package com.chacha.multitenantsaas.taskrelationships.repository;

import com.chacha.multitenantsaas.taskrelationships.entity.TaskDependency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {

    boolean existsByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
            UUID tenantId, UUID projectId, UUID blockingTaskId, UUID dependentTaskId);

    Optional<TaskDependency> findByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
            UUID tenantId, UUID projectId, UUID blockingTaskId, UUID dependentTaskId);

    @EntityGraph(attributePaths = {"blockingTask", "dependentTask"})
    List<TaskDependency> findByTenant_IdAndProject_Id(
            UUID tenantId, UUID projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"blockingTask", "dependentTask"})
    List<TaskDependency> findByTenant_IdAndProject_IdAndDependentTask_IdOrderByCreatedAtAsc(
            UUID tenantId, UUID projectId, UUID dependentTaskId, Pageable pageable);

    @EntityGraph(attributePaths = {"blockingTask", "dependentTask"})
    List<TaskDependency> findByTenant_IdAndProject_IdAndBlockingTask_IdOrderByCreatedAtAsc(
            UUID tenantId, UUID projectId, UUID blockingTaskId, Pageable pageable);
}
