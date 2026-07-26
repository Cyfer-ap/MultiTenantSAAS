package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.entity.*;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProjectTaskService {

    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentActorService currentActorService;

    public ProjectTaskService(
            ProjectTaskRepository projectTaskRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            AppUserRepository appUserRepository,
            CurrentActorService currentActorService
    ) {
        this.projectTaskRepository = projectTaskRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.appUserRepository = appUserRepository;
        this.currentActorService = currentActorService;
    }

    @Transactional
    public ProjectTaskResponse createTask(
            UUID tenantId,
            UUID projectId,
            ProjectTaskCreateRequest request,
            Jwt jwt
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectCanBeModified(project);

        AppUser creator =
                currentActorService.getRequiredActiveActor(
                        tenantId,
                        jwt
                );

        AppUser assignee = resolveAssignee(
                tenantId,
                projectId,
                request.assigneeUserId()
        );

        ProjectTask task = new ProjectTask(
                project.getTenant(),
                project,
                creator,
                assignee,
                request.title().trim(),
                normalizeDescription(request.description()),
                request.priority(),
                request.dueAt()
        );

        return mapToResponse(
                projectTaskRepository.save(task)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectTaskResponse> getTasks(
            UUID tenantId,
            UUID projectId,
            ProjectTaskStatus status,
            ProjectTaskPriority priority,
            UUID assigneeUserId,
            String search,
            Pageable pageable
    ) {
        getProjectOrThrow(tenantId, projectId);

        Page<ProjectTask> tasks =
                projectTaskRepository.findProjectTasks(
                        tenantId,
                        projectId,
                        status,
                        priority,
                        assigneeUserId,
                        normalizeSearch(search),
                        pageable
                );

        return new PageResponse<>(
                tasks.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                tasks.getNumber(),
                tasks.getSize(),
                tasks.getTotalElements(),
                tasks.getTotalPages(),
                tasks.isFirst(),
                tasks.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ProjectTaskResponse getTask(
            UUID tenantId,
            UUID projectId,
            UUID taskId
    ) {
        return mapToResponse(
                getTaskOrThrow(
                        tenantId,
                        projectId,
                        taskId
                )
        );
    }

    @Transactional
    public ProjectTaskResponse updateTask(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            ProjectTaskUpdateRequest request
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectCanBeModified(project);

        ProjectTask task =
                getTaskOrThrow(
                        tenantId,
                        projectId,
                        taskId
                );

        ensureTaskCanBeModified(task);

        task.setTitle(request.title().trim());
        task.setDescription(
                normalizeDescription(request.description())
        );
        task.setPriority(request.priority());
        task.setDueAt(request.dueAt());

        return mapToResponse(
                projectTaskRepository.save(task)
        );
    }

    @Transactional
    public ProjectTaskResponse updateTaskStatus(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            ProjectTaskStatusUpdateRequest request
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectCanBeModified(project);

        ProjectTask task =
                getTaskOrThrow(
                        tenantId,
                        projectId,
                        taskId
                );

        ensureTaskCanBeModified(task);

        if (request.status() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Use the cancel endpoint to cancel a task"
            );
        }

        task.setStatus(request.status());

        if (request.status()
                == ProjectTaskStatus.COMPLETED) {
            task.setCompletedAt(Instant.now());
        } else {
            task.setCompletedAt(null);
        }

        return mapToResponse(
                projectTaskRepository.save(task)
        );
    }

    @Transactional
    public ProjectTaskResponse updateAssignee(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            ProjectTaskAssigneeUpdateRequest request
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectCanBeModified(project);

        ProjectTask task =
                getTaskOrThrow(
                        tenantId,
                        projectId,
                        taskId
                );

        ensureTaskCanBeModified(task);

        AppUser assignee = resolveAssignee(
                tenantId,
                projectId,
                request.assigneeUserId()
        );

        task.setAssigneeUser(assignee);

        return mapToResponse(
                projectTaskRepository.save(task)
        );
    }

    @Transactional
    public ProjectTaskResponse cancelTask(
            UUID tenantId,
            UUID projectId,
            UUID taskId
    ) {
        Project project =
                getProjectOrThrow(tenantId, projectId);

        ensureProjectCanBeModified(project);

        ProjectTask task =
                getTaskOrThrow(
                        tenantId,
                        projectId,
                        taskId
                );

        ensureTaskCanBeModified(task);

        task.setStatus(ProjectTaskStatus.CANCELLED);
        task.setCompletedAt(null);

        return mapToResponse(
                projectTaskRepository.save(task)
        );
    }

    private AppUser resolveAssignee(
            UUID tenantId,
            UUID projectId,
            UUID assigneeUserId
    ) {
        if (assigneeUserId == null) {
            return null;
        }

        AppUser assignee = appUserRepository
                .findByTenantIdAndId(
                        tenantId,
                        assigneeUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignee not found with id: "
                                + assigneeUserId
                ));

        if (assignee.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only active users can be assigned tasks"
            );
        }

        boolean projectMember = projectMemberRepository
                .existsByProject_Tenant_IdAndProject_IdAndUser_Id(
                        tenantId,
                        projectId,
                        assigneeUserId
                );

        if (!projectMember) {
            throw new IllegalArgumentException(
                    "Task assignee must be a member of the project"
            );
        }

        return assignee;
    }

    private Project getProjectOrThrow(
            UUID tenantId,
            UUID projectId
    ) {
        return projectRepository
                .findByTenant_IdAndId(
                        tenantId,
                        projectId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: "
                                + projectId
                                + " for tenant: "
                                + tenantId
                ));
    }

    private ProjectTask getTaskOrThrow(
            UUID tenantId,
            UUID projectId,
            UUID taskId
    ) {
        return projectTaskRepository
                .findByProject_Tenant_IdAndProject_IdAndId(
                        tenantId,
                        projectId,
                        taskId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found with id: "
                                + taskId
                                + " for project: "
                                + projectId
                ));
    }

    private void ensureProjectCanBeModified(
            Project project
    ) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Archived project tasks cannot be modified"
            );
        }
    }

    private void ensureTaskCanBeModified(
            ProjectTask task
    ) {
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cancelled task cannot be modified"
            );
        }
    }

    private String normalizeDescription(
            String description
    ) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }

        String normalized = search.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private ProjectTaskResponse mapToResponse(
            ProjectTask task
    ) {
        AppUser creator = task.getCreatedByUser();
        AppUser assignee = task.getAssigneeUser();

        return new ProjectTaskResponse(
                task.getId(),
                task.getTenant().getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                assignee == null ? null : assignee.getEmail(),
                creator.getId(),
                creator.getFullName(),
                creator.getEmail(),
                task.getDueAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}