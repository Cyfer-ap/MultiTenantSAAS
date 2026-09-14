package com.chacha.multitenantsaas.taskrelationships.repository;

import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTaskLabelRepository extends JpaRepository<ProjectTaskLabel, UUID> {

    List<ProjectTaskLabel> findByTenant_IdAndProject_IdOrderByNameAsc(UUID tenantId, UUID projectId);

    Optional<ProjectTaskLabel> findByTenant_IdAndProject_IdAndId(
            UUID tenantId, UUID projectId, UUID labelId);

    boolean existsByTenant_IdAndProject_IdAndNormalizedName(
            UUID tenantId, UUID projectId, String normalizedName);

    boolean existsByTenant_IdAndProject_IdAndNormalizedNameAndIdNot(
            UUID tenantId, UUID projectId, String normalizedName, UUID labelId);
}
