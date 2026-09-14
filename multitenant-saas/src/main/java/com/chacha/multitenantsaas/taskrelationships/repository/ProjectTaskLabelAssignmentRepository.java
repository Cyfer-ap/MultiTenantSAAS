package com.chacha.multitenantsaas.taskrelationships.repository;

import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabelAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTaskLabelAssignmentRepository
        extends JpaRepository<ProjectTaskLabelAssignment, UUID> {

    @EntityGraph(attributePaths = "label")
    List<ProjectTaskLabelAssignment> findByTenant_IdAndProject_IdAndTask_IdOrderByAssignedAtAsc(
            UUID tenantId, UUID projectId, UUID taskId);

    Optional<ProjectTaskLabelAssignment>
            findByTenant_IdAndProject_IdAndTask_IdAndLabel_Id(
                    UUID tenantId, UUID projectId, UUID taskId, UUID labelId);

    void deleteByTenant_IdAndProject_IdAndLabel_Id(UUID tenantId, UUID projectId, UUID labelId);
}
