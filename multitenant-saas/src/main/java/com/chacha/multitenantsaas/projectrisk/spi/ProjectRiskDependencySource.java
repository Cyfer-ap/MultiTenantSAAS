package com.chacha.multitenantsaas.projectrisk.spi;

import java.util.List;
import java.util.UUID;

public interface ProjectRiskDependencySource {

    List<DependencySnapshot> findProjectDependencies(UUID tenantId, UUID projectId, int limit);

    record DependencySnapshot(UUID blockingTaskId, UUID dependentTaskId) {}
}
