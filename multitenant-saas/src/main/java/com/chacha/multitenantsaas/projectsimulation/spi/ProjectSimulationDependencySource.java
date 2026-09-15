package com.chacha.multitenantsaas.projectsimulation.spi;

import java.util.List;
import java.util.UUID;

public interface ProjectSimulationDependencySource {

    List<DependencySnapshot> findProjectDependencies(UUID tenantId, UUID projectId, int limit);

    record DependencySnapshot(UUID blockingTaskId, UUID dependentTaskId) {}
}
