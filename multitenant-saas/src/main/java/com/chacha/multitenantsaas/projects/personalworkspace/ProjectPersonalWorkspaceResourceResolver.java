package com.chacha.multitenantsaas.projects.personalworkspace;

import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalWorkspaceResource;
import com.chacha.multitenantsaas.personalworkspace.spi.PersonalWorkspaceResourceResolver;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProjectPersonalWorkspaceResourceResolver implements PersonalWorkspaceResourceResolver {

    private final ProjectRepository projectRepository;
    private final AuthorizationSecurityService authorizationSecurity;

    public ProjectPersonalWorkspaceResourceResolver(
            ProjectRepository projectRepository,
            AuthorizationSecurityService authorizationSecurity) {
        this.projectRepository = projectRepository;
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public PersonalResourceType resourceType() {
        return PersonalResourceType.PROJECT;
    }

    @Override
    public Optional<PersonalWorkspaceResource> resolve(
            UUID tenantId, UUID userId, UUID resourceId) {
        if (!authorizationSecurity.hasProjectPermission(
                tenantId, resourceId, PlatformPermissionCodes.PROJECT_READ)) {
            return Optional.empty();
        }

        return projectRepository
                .findByTenant_IdAndId(tenantId, resourceId)
                .map(
                        project ->
                                new PersonalWorkspaceResource(
                                        PersonalResourceType.PROJECT,
                                        project.getId(),
                                        null,
                                        project.getName(),
                                        project.getStatus().name()));
    }
}
