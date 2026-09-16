package com.chacha.multitenantsaas.forms;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.projects.access.ProjectAccessPort;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.workflows.WorkflowFormSubmissionPort;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormDefinitionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FormDefinitionRepository definitionRepository;
    private final ProjectAccessPort projectAccessPort;
    private final CurrentActorService currentActorService;
    private final FormSchemaEngine schemaEngine;
    private final WorkflowFormSubmissionPort workflowPort;

    public FormDefinitionService(
            FormDefinitionRepository definitionRepository,
            ProjectAccessPort projectAccessPort,
            CurrentActorService currentActorService,
            FormSchemaEngine schemaEngine,
            WorkflowFormSubmissionPort workflowPort) {
        this.definitionRepository = definitionRepository;
        this.projectAccessPort = projectAccessPort;
        this.currentActorService = currentActorService;
        this.schemaEngine = schemaEngine;
        this.workflowPort = workflowPort;
    }

    @Transactional(readOnly = true)
    public PageResponse<FormDtos.SummaryResponse> list(
            UUID tenantId, UUID projectId, Pageable pageable) {
        Page<FormDefinition> page =
                definitionRepository.findByTenantIdAndProjectIdOrderByNameAsc(
                        tenantId, projectId, bounded(pageable));
        return new PageResponse<>(
                page.getContent().stream().map(this::mapSummary).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Transactional(readOnly = true)
    public FormDtos.Response get(UUID tenantId, UUID projectId, UUID formId) {
        return map(requireDetailed(tenantId, projectId, formId));
    }

    @Transactional
    public FormDtos.Response create(
            UUID tenantId, UUID projectId, FormDtos.UpsertRequest request, Jwt jwt) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        schemaEngine.validateDefinition(request);
        validateWorkflowTarget(tenantId, request.workflowId());
        String normalizedName = normalizeName(request.name());
        if (definitionRepository.existsByTenantIdAndProjectIdAndNormalizedName(
                tenantId, projectId, normalizedName)) {
            throw duplicateName();
        }

        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        FormDefinition definition =
                new FormDefinition(
                        tenantId,
                        projectId,
                        actorUserId,
                        request.name().trim(),
                        normalizedName,
                        normalizeDescription(request.description()),
                        request.taskTitleFieldKey(),
                        blankToNull(request.taskDescriptionFieldKey()),
                        blankToNull(request.taskDueDateFieldKey()),
                        request.taskPriority(),
                        request.workflowId());
        definition.replaceFields(buildFields(definition, request.fields()));
        return map(definitionRepository.saveAndFlush(definition));
    }

    @Transactional
    public FormDtos.Response update(
            UUID tenantId,
            UUID projectId,
            UUID formId,
            FormDtos.UpsertRequest request) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        FormDefinition definition = requireDetailed(tenantId, projectId, formId);
        if (definition.getStatus() == FormStatus.ACTIVE) {
            throw new IllegalStateException("Active forms must be paused before editing");
        }
        schemaEngine.validateDefinition(request);
        validateWorkflowTarget(tenantId, request.workflowId());
        String normalizedName = normalizeName(request.name());
        if (definitionRepository.existsByTenantIdAndProjectIdAndNormalizedNameAndIdNot(
                tenantId, projectId, normalizedName, formId)) {
            throw duplicateName();
        }

        definition.update(
                request.name().trim(),
                normalizedName,
                normalizeDescription(request.description()),
                request.taskTitleFieldKey(),
                blankToNull(request.taskDescriptionFieldKey()),
                blankToNull(request.taskDueDateFieldKey()),
                request.taskPriority(),
                request.workflowId());
        definition.replaceFields(List.of());
        definitionRepository.saveAndFlush(definition);
        definition.replaceFields(buildFields(definition, request.fields()));
        return map(definitionRepository.saveAndFlush(definition));
    }

    @Transactional
    public FormDtos.Response activate(UUID tenantId, UUID projectId, UUID formId) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        FormDefinition definition = requireDetailed(tenantId, projectId, formId);
        schemaEngine.validateStored(definition);
        validateWorkflowTarget(tenantId, definition.getWorkflowId());
        definition.activate();
        return map(definitionRepository.saveAndFlush(definition));
    }

    @Transactional
    public FormDtos.Response pause(UUID tenantId, UUID projectId, UUID formId) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        FormDefinition definition = requireDetailed(tenantId, projectId, formId);
        definition.pause();
        return map(definitionRepository.saveAndFlush(definition));
    }

    private List<FormField> buildFields(
            FormDefinition definition, List<FormDtos.FieldRequest> requests) {
        java.util.ArrayList<FormField> fields = new java.util.ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            FormDtos.FieldRequest request = requests.get(index);
            List<String> options = schemaEngine.normalizeOptions(request.type(), request.options());
            fields.add(
                    new FormField(
                            definition,
                            request.key(),
                            request.label().trim(),
                            request.type(),
                            request.required(),
                            options.isEmpty() ? null : schemaEngine.writeOptions(options),
                            index));
        }
        return fields;
    }

    private void validateWorkflowTarget(UUID tenantId, UUID workflowId) {
        if (workflowId != null) {
            workflowPort.requireFormSubmissionTarget(tenantId, workflowId);
        }
    }

    private FormDefinition requireDetailed(UUID tenantId, UUID projectId, UUID formId) {
        return definitionRepository
                .findDetailed(tenantId, projectId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formId));
    }

    private FormDtos.Response map(FormDefinition definition) {
        List<FormDtos.FieldResponse> fields =
                definition.getFields().stream()
                        .map(
                                field ->
                                        new FormDtos.FieldResponse(
                                                field.getId(),
                                                field.getFieldKey(),
                                                field.getLabel(),
                                                field.getFieldType(),
                                                field.isRequired(),
                                                schemaEngine.readOptions(field.getOptionsJson()),
                                                field.getPositionIndex()))
                        .toList();
        return new FormDtos.Response(
                definition.getId(),
                definition.getTenantId(),
                definition.getProjectId(),
                definition.getCreatedByUserId(),
                definition.getName(),
                definition.getDescription(),
                definition.getStatus(),
                definition.getDefinitionVersion(),
                fields,
                definition.getTaskTitleFieldKey(),
                definition.getTaskDescriptionFieldKey(),
                definition.getTaskDueDateFieldKey(),
                definition.getTaskPriority(),
                definition.getWorkflowId(),
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }

    private FormDtos.SummaryResponse mapSummary(FormDefinition definition) {
        return new FormDtos.SummaryResponse(
                definition.getId(),
                definition.getTenantId(),
                definition.getProjectId(),
                definition.getName(),
                definition.getStatus(),
                definition.getDefinitionVersion(),
                definition.getWorkflowId(),
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }

    private Pageable bounded(Pageable pageable) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size);
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DuplicateResourceException duplicateName() {
        return new DuplicateResourceException("A form with this name already exists in the project");
    }
}
