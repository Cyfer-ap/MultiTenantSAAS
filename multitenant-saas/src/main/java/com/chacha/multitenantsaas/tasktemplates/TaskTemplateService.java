package com.chacha.multitenantsaas.tasktemplates;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskTemplateService {

    private static final int MAX_TEMPLATES_PER_PROJECT = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectTaskTemplateRepository templateRepository;
    private final CurrentActorService currentActorService;
    private final TaskCreationPort taskCreationPort;

    public TaskTemplateService(
            ProjectTaskTemplateRepository templateRepository,
            CurrentActorService currentActorService,
            TaskCreationPort taskCreationPort) {
        this.templateRepository = templateRepository;
        this.currentActorService = currentActorService;
        this.taskCreationPort = taskCreationPort;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskTemplateDtos.Response> list(
            UUID tenantId, UUID projectId, Pageable pageable) {
        Page<ProjectTaskTemplate> page =
                templateRepository.findByTenantIdAndProjectIdOrderByNameAsc(
                        tenantId, projectId, bounded(pageable));
        return new PageResponse<>(
                page.getContent().stream().map(this::map).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Transactional(readOnly = true)
    public TaskTemplateDtos.Response get(UUID tenantId, UUID projectId, UUID templateId) {
        return map(requireScoped(tenantId, projectId, templateId));
    }

    @Transactional
    public TaskTemplateDtos.Response create(
            UUID tenantId,
            UUID projectId,
            TaskTemplateDtos.UpsertRequest request,
            Jwt jwt) {
        if (templateRepository.countByTenantIdAndProjectId(tenantId, projectId)
                >= MAX_TEMPLATES_PER_PROJECT) {
            throw new IllegalStateException("A project can have at most 100 task templates");
        }
        String normalizedName = normalizeName(request.name());
        if (templateRepository.existsByTenantIdAndProjectIdAndNormalizedName(
                tenantId, projectId, normalizedName)) {
            throw new IllegalArgumentException("A task template with this name already exists");
        }
        UUID creatorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        ProjectTaskTemplate template =
                new ProjectTaskTemplate(
                        tenantId,
                        projectId,
                        creatorUserId,
                        request.assigneeUserId(),
                        request.name().trim(),
                        normalizedName,
                        request.taskTitle().trim(),
                        normalizeDescription(request.taskDescription()),
                        request.priority(),
                        request.dueOffsetMinutes());
        return map(templateRepository.save(template));
    }

    @Transactional
    public TaskTemplateDtos.Response update(
            UUID tenantId,
            UUID projectId,
            UUID templateId,
            TaskTemplateDtos.UpsertRequest request) {
        ProjectTaskTemplate template = requireScoped(tenantId, projectId, templateId);
        String normalizedName = normalizeName(request.name());
        if (templateRepository.existsByTenantIdAndProjectIdAndNormalizedNameAndIdNot(
                tenantId, projectId, normalizedName, templateId)) {
            throw new IllegalArgumentException("A task template with this name already exists");
        }
        template.update(
                request.assigneeUserId(),
                request.name().trim(),
                normalizedName,
                request.taskTitle().trim(),
                normalizeDescription(request.taskDescription()),
                request.priority(),
                request.dueOffsetMinutes());
        return map(templateRepository.save(template));
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID templateId) {
        templateRepository.delete(requireScoped(tenantId, projectId, templateId));
    }

    @Transactional
    public TaskTemplateDtos.InstantiateResponse instantiate(
            UUID tenantId, UUID projectId, UUID templateId, Jwt jwt) {
        ProjectTaskTemplate template = requireScoped(tenantId, projectId, templateId);
        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        Instant now = Instant.now();
        Instant dueAt =
                template.getDueOffsetMinutes() == null
                        ? null
                        : now.plusSeconds(template.getDueOffsetMinutes() * 60L);
        TaskCreationResult result =
                taskCreationPort.createTask(
                        new TaskCreationCommand(
                                tenantId,
                                projectId,
                                actorUserId,
                                template.getAssigneeUserId(),
                                template.getTaskTitle(),
                                template.getTaskDescription(),
                                template.getPriority(),
                                dueAt,
                                "Task created from template \"" + template.getName() + "\""));
        return new TaskTemplateDtos.InstantiateResponse(
                template.getId(), result.taskId(), result.createdAt());
    }

    private ProjectTaskTemplate requireScoped(UUID tenantId, UUID projectId, UUID templateId) {
        return templateRepository
                .findByTenantIdAndProjectIdAndId(tenantId, projectId, templateId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task template not found: " + templateId));
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                pageable.getSort());
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private TaskTemplateDtos.Response map(ProjectTaskTemplate template) {
        return new TaskTemplateDtos.Response(
                template.getId(),
                template.getTenantId(),
                template.getProjectId(),
                template.getCreatedByUserId(),
                template.getAssigneeUserId(),
                template.getName(),
                template.getTaskTitle(),
                template.getTaskDescription(),
                template.getPriority(),
                template.getDueOffsetMinutes(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
