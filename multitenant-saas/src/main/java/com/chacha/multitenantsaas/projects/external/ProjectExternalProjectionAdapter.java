package com.chacha.multitenantsaas.projects.external;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.externalaccess.ExternalProjectProjectionPort;
import com.chacha.multitenantsaas.externalaccess.ExternalProjectSnapshot;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectExternalProjectionAdapter implements ExternalProjectProjectionPort {

    private final ProjectRepository projectRepository;

    public ProjectExternalProjectionAdapter(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalProjectSnapshot requireProject(UUID tenantId, UUID projectId) {
        Project project =
                projectRepository
                        .findByTenant_IdAndId(tenantId, projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return new ExternalProjectSnapshot(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getUpdatedAt());
    }
}
