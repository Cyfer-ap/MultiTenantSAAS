package com.chacha.multitenantsaas.tasks.simulation;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationTaskSource;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskProjectSimulationSource implements ProjectSimulationTaskSource {

    private final ProjectTaskRepository taskRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskProjectSimulationSource(
            ProjectTaskRepository taskRepository,
            AppUserRepository appUserRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.taskRepository = taskRepository;
        this.appUserRepository = appUserRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSnapshot> findProjectTasks(UUID tenantId, UUID projectId, int requestedLimit) {
        int limit = Math.max(1, requestedLimit);
        var pageable =
                PageRequest.of(
                        0, limit, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        return taskRepository
                .findProjectTasks(tenantId, projectId, null, null, null, null, pageable)
                .stream()
                .map(this::snapshot)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssigneeSnapshot> findAssignableAssignee(
            UUID tenantId, UUID projectId, UUID userId) {
        if (!projectMemberRepository.existsByProject_Tenant_IdAndProject_IdAndUser_Id(
                tenantId, projectId, userId)) {
            return Optional.empty();
        }
        return appUserRepository
                .findByTenantIdAndId(tenantId, userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(user -> new AssigneeSnapshot(user.getId(), user.getFullName()));
    }

    private TaskSnapshot snapshot(ProjectTask task) {
        AppUser assignee = task.getAssigneeUser();
        return new TaskSnapshot(
                task.getId(),
                task.getTitle(),
                task.getStatus().name(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                task.getDueAt());
    }
}
