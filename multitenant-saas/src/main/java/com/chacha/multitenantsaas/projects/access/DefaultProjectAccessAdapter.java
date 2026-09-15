package com.chacha.multitenantsaas.projects.access;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultProjectAccessAdapter implements ProjectAccessPort {

    private final ProjectRepository projectRepository;

    public DefaultProjectAccessAdapter(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAccessSnapshot requireProject(UUID tenantId, UUID projectId) {
        Project project =
                projectRepository
                        .findByTenant_IdAndId(tenantId, projectId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Project not found: " + projectId));
        return new ProjectAccessSnapshot(project.getId(), project.getStatus());
    }
}
