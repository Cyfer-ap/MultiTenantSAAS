package com.chacha.multitenantsaas.taskrelationships.risk;

import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskDependencySource;
import com.chacha.multitenantsaas.taskrelationships.repository.TaskDependencyRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskRelationshipProjectRiskSource implements ProjectRiskDependencySource {

    private final TaskDependencyRepository dependencyRepository;

    public TaskRelationshipProjectRiskSource(TaskDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DependencySnapshot> findProjectDependencies(
            UUID tenantId, UUID projectId, int requestedLimit) {
        int limit = Math.max(1, requestedLimit);
        var pageable =
                PageRequest.of(
                        0, limit, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        return dependencyRepository
                .findByTenant_IdAndProject_Id(tenantId, projectId, pageable)
                .stream()
                .map(
                        dependency ->
                                new DependencySnapshot(
                                        dependency.getBlockingTask().getId(),
                                        dependency.getDependentTask().getId()))
                .toList();
    }
}
