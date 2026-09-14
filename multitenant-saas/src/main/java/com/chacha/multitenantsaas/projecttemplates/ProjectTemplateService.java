package com.chacha.multitenantsaas.projecttemplates;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationCommand;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationPort;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationResult;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectTemplateService {

    private static final int MAX_TEMPLATES_PER_TENANT = 100;
    private static final int MAX_TASKS_PER_TEMPLATE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectTemplateRepository templateRepository;
    private final ProjectTemplateTaskRepository taskRepository;
    private final CurrentActorService currentActorService;
    private final ProjectCreationPort projectCreationPort;
    private final TaskCreationPort taskCreationPort;

    public ProjectTemplateService(
            ProjectTemplateRepository templateRepository,
            ProjectTemplateTaskRepository taskRepository,
            CurrentActorService currentActorService,
            ProjectCreationPort projectCreationPort,
            TaskCreationPort taskCreationPort) {
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.currentActorService = currentActorService;
        this.projectCreationPort = projectCreationPort;
        this.taskCreationPort = taskCreationPort;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectTemplateDtos.Response> list(UUID tenantId, Pageable pageable) {
        Page<ProjectTemplate> page =
                templateRepository.findByTenantIdOrderByNameAsc(tenantId, bounded(pageable));
        List<UUID> templateIds = page.getContent().stream().map(ProjectTemplate::getId).toList();
        Map<UUID, List<ProjectTemplateTask>> tasksByTemplate =
                loadTasksByTemplate(tenantId, templateIds);

        return new PageResponse<>(
                page.getContent().stream()
                        .map(
                                template ->
                                        map(
                                                template,
                                                tasksByTemplate.getOrDefault(
                                                        template.getId(), List.of())))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Transactional(readOnly = true)
    public ProjectTemplateDtos.Response get(UUID tenantId, UUID templateId) {
        ProjectTemplate template = requireScoped(tenantId, templateId);
        return map(
                template,
                taskRepository.findByTenantIdAndTemplateIdOrderByPositionIndexAsc(
                        tenantId, templateId));
    }

    @Transactional
    public ProjectTemplateDtos.Response create(
            UUID tenantId, ProjectTemplateDtos.UpsertRequest request, Jwt jwt) {
        if (templateRepository.countByTenantId(tenantId) >= MAX_TEMPLATES_PER_TENANT) {
            throw new IllegalStateException("A tenant can have at most 100 project templates");
        }
        validateTaskCount(request.tasks());
        validateInitialStatus(request.initialStatus());

        String normalizedName = normalizeTemplateName(request.name());
        if (templateRepository.existsByTenantIdAndNormalizedName(tenantId, normalizedName)) {
            throw new IllegalArgumentException("A project template with this name already exists");
        }

        UUID creatorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        ProjectTemplate template =
                new ProjectTemplate(
                        tenantId,
                        creatorUserId,
                        request.name().trim(),
                        normalizedName,
                        request.projectNameSeed().trim(),
                        normalizeDescription(request.projectDescription()),
                        request.initialStatus());
        ProjectTemplate saved = templateRepository.save(template);
        List<ProjectTemplateTask> tasks = saveSnapshots(saved, request.tasks());
        return map(saved, tasks);
    }

    @Transactional
    public ProjectTemplateDtos.Response update(
            UUID tenantId, UUID templateId, ProjectTemplateDtos.UpsertRequest request) {
        ProjectTemplate template = requireScoped(tenantId, templateId);
        validateTaskCount(request.tasks());
        validateInitialStatus(request.initialStatus());

        String normalizedName = normalizeTemplateName(request.name());
        if (templateRepository.existsByTenantIdAndNormalizedNameAndIdNot(
                tenantId, normalizedName, templateId)) {
            throw new IllegalArgumentException("A project template with this name already exists");
        }

        template.update(
                request.name().trim(),
                normalizedName,
                request.projectNameSeed().trim(),
                normalizeDescription(request.projectDescription()),
                request.initialStatus());
        ProjectTemplate saved = templateRepository.save(template);
        taskRepository.deleteSnapshots(tenantId, templateId);
        List<ProjectTemplateTask> tasks = saveSnapshots(saved, request.tasks());
        return map(saved, tasks);
    }

    @Transactional
    public void delete(UUID tenantId, UUID templateId) {
        templateRepository.delete(requireScoped(tenantId, templateId));
    }

    @Transactional
    public ProjectTemplateDtos.InstantiateResponse instantiate(
            UUID tenantId,
            UUID templateId,
            ProjectTemplateDtos.InstantiateRequest request,
            Jwt jwt) {
        ProjectTemplate template = requireScoped(tenantId, templateId);
        List<ProjectTemplateTask> snapshots =
                taskRepository.findByTenantIdAndTemplateIdOrderByPositionIndexAsc(
                        tenantId, templateId);
        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();

        String projectName =
                request == null || request.projectName() == null
                        ? template.getProjectNameSeed()
                        : request.projectName().trim();
        if (projectName.isBlank()) {
            projectName = template.getProjectNameSeed();
        }

        ProjectCreationResult project =
                projectCreationPort.createProject(
                        new ProjectCreationCommand(
                                tenantId,
                                actorUserId,
                                projectName,
                                template.getProjectDescription(),
                                template.getInitialStatus()));

        for (ProjectTemplateTask snapshot : snapshots) {
            Instant dueAt =
                    snapshot.getDueOffsetMinutes() == null
                            ? null
                            : project.createdAt().plusSeconds(snapshot.getDueOffsetMinutes() * 60L);
            taskCreationPort.createTask(
                    new TaskCreationCommand(
                            tenantId,
                            project.projectId(),
                            actorUserId,
                            null,
                            snapshot.getTitle(),
                            snapshot.getDescription(),
                            snapshot.getPriority(),
                            dueAt,
                            "Task created from project template \"" + template.getName() + "\""));
        }

        return new ProjectTemplateDtos.InstantiateResponse(
                templateId, project.projectId(), snapshots.size(), project.createdAt());
    }

    private ProjectTemplate requireScoped(UUID tenantId, UUID templateId) {
        return templateRepository
                .findByTenantIdAndId(tenantId, templateId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Project template not found: " + templateId));
    }

    private List<ProjectTemplateTask> saveSnapshots(
            ProjectTemplate template, List<ProjectTemplateDtos.TaskSnapshotRequest> requests) {
        List<ProjectTemplateTask> snapshots = new ArrayList<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            ProjectTemplateDtos.TaskSnapshotRequest request = requests.get(index);
            snapshots.add(
                    new ProjectTemplateTask(
                            template.getTenantId(),
                            template.getId(),
                            index,
                            request.title().trim(),
                            normalizeDescription(request.description()),
                            request.priority(),
                            request.dueOffsetMinutes()));
        }
        return taskRepository.saveAll(snapshots);
    }

    private Map<UUID, List<ProjectTemplateTask>> loadTasksByTemplate(
            UUID tenantId, List<UUID> templateIds) {
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        return taskRepository
                .findByTenantIdAndTemplateIdInOrderByTemplateIdAscPositionIndexAsc(
                        tenantId, templateIds)
                .stream()
                .collect(Collectors.groupingBy(ProjectTemplateTask::getTemplateId));
    }

    private void validateTaskCount(List<ProjectTemplateDtos.TaskSnapshotRequest> tasks) {
        if (tasks == null) {
            throw new IllegalArgumentException("Project template tasks are required");
        }
        if (tasks.size() > MAX_TASKS_PER_TEMPLATE) {
            throw new IllegalArgumentException("A project template can contain at most 50 tasks");
        }
    }

    private void validateInitialStatus(ProjectStatus status) {
        if (status == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "A project template cannot create archived projects");
        }
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                pageable.getSort());
    }

    private String normalizeTemplateName(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private ProjectTemplateDtos.Response map(
            ProjectTemplate template, List<ProjectTemplateTask> tasks) {
        return new ProjectTemplateDtos.Response(
                template.getId(),
                template.getTenantId(),
                template.getCreatedByUserId(),
                template.getName(),
                template.getProjectNameSeed(),
                template.getProjectDescription(),
                template.getInitialStatus(),
                tasks.stream()
                        .map(
                                task ->
                                        new ProjectTemplateDtos.TaskSnapshotResponse(
                                                task.getId(),
                                                task.getPositionIndex(),
                                                task.getTitle(),
                                                task.getDescription(),
                                                task.getPriority(),
                                                task.getDueOffsetMinutes()))
                        .toList(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
