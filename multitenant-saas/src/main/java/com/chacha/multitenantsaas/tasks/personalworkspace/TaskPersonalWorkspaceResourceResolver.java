package com.chacha.multitenantsaas.tasks.personalworkspace;

import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalWorkspaceResource;
import com.chacha.multitenantsaas.personalworkspace.spi.PersonalWorkspaceResourceResolver;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TaskPersonalWorkspaceResourceResolver implements PersonalWorkspaceResourceResolver {

    private final ProjectTaskRepository projectTaskRepository;
    private final AuthorizationSecurityService authorizationSecurity;

    public TaskPersonalWorkspaceResourceResolver(
            ProjectTaskRepository projectTaskRepository,
            AuthorizationSecurityService authorizationSecurity) {
        this.projectTaskRepository = projectTaskRepository;
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public PersonalResourceType resourceType() {
        return PersonalResourceType.TASK;
    }

    @Override
    public Optional<PersonalWorkspaceResource> resolve(
            UUID tenantId, UUID userId, UUID resourceId) {
        return projectTaskRepository
                .findByTenant_IdAndId(tenantId, resourceId)
                .filter(
                        task ->
                                authorizationSecurity.canReadProjectTasks(
                                        tenantId,
                                        task.getProject().getId(),
                                        PlatformPermissionCodes.PROJECT_TASK_READ))
                .map(
                        task ->
                                new PersonalWorkspaceResource(
                                        PersonalResourceType.TASK,
                                        task.getId(),
                                        task.getProject().getId(),
                                        task.getTitle(),
                                        task.getProject().getName()));
    }
}
