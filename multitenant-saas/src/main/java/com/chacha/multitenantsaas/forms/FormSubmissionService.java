package com.chacha.multitenantsaas.forms;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormSubmissionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FormDefinitionRepository definitionRepository;
    private final FormSubmissionRepository submissionRepository;
    private final CurrentActorService currentActorService;
    private final TaskCreationPort taskCreationPort;
    private final FormSchemaEngine schemaEngine;
    private final ApplicationEventPublisher eventPublisher;

    public FormSubmissionService(
            FormDefinitionRepository definitionRepository,
            FormSubmissionRepository submissionRepository,
            CurrentActorService currentActorService,
            TaskCreationPort taskCreationPort,
            FormSchemaEngine schemaEngine,
            ApplicationEventPublisher eventPublisher) {
        this.definitionRepository = definitionRepository;
        this.submissionRepository = submissionRepository;
        this.currentActorService = currentActorService;
        this.taskCreationPort = taskCreationPort;
        this.schemaEngine = schemaEngine;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FormDtos.SubmissionResponse submit(
            UUID tenantId,
            UUID projectId,
            UUID formId,
            FormDtos.SubmissionRequest request,
            Jwt jwt) {
        FormDefinition definition = requireForm(tenantId, projectId, formId);
        if (definition.getStatus() != FormStatus.ACTIVE) {
            throw new IllegalStateException("Only active forms accept submissions");
        }
        schemaEngine.validateStored(definition);
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        Map<String, Object> values = schemaEngine.validateSubmission(definition, request.values());

        String title = requiredText(values, definition.getTaskTitleFieldKey(), "task title");
        if (title.length() > 200) {
            throw new IllegalArgumentException("Generated task title cannot exceed 200 characters");
        }
        String description = optionalText(values, definition.getTaskDescriptionFieldKey());
        java.time.Instant dueAt = dueAt(values, definition.getTaskDueDateFieldKey());
        UUID submissionId = UUID.randomUUID();

        TaskCreationResult created =
                taskCreationPort.createTask(
                        new TaskCreationCommand(
                                tenantId,
                                projectId,
                                actor.getId(),
                                null,
                                title,
                                description,
                                definition.getTaskPriority(),
                                dueAt,
                                "Task created from form '"
                                        + definition.getName()
                                        + "' submission "
                                        + submissionId));

        String validationContext =
                "definitionVersion="
                        + definition.getDefinitionVersion()
                        + ";fieldCount="
                        + definition.getFields().size()
                        + ";payloadValidated=true";
        FormSubmission submission =
                submissionRepository.saveAndFlush(
                        new FormSubmission(
                                submissionId,
                                tenantId,
                                projectId,
                                formId,
                                definition.getDefinitionVersion(),
                                actor.getId(),
                                schemaEngine.writePayload(values),
                                created.taskId(),
                                validationContext));

        if (definition.getWorkflowId() != null) {
            eventPublisher.publishEvent(
                    new FormSubmissionAcceptedEvent(
                            tenantId,
                            projectId,
                            formId,
                            submissionId,
                            created.taskId(),
                            actor.getId(),
                            definition.getTaskPriority(),
                            definition.getWorkflowId()));
        }
        return map(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<FormDtos.SubmissionResponse> history(
            UUID tenantId, UUID projectId, UUID formId, Pageable pageable) {
        requireForm(tenantId, projectId, formId);
        Page<FormSubmission> page =
                submissionRepository.findByTenantIdAndProjectIdAndFormIdOrderBySubmittedAtDesc(
                        tenantId, projectId, formId, bounded(pageable));
        return new PageResponse<>(
                page.getContent().stream().map(this::map).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private FormDefinition requireForm(UUID tenantId, UUID projectId, UUID formId) {
        return definitionRepository
                .findDetailed(tenantId, projectId, formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formId));
    }

    private FormDtos.SubmissionResponse map(FormSubmission submission) {
        return new FormDtos.SubmissionResponse(
                submission.getId(),
                submission.getFormId(),
                submission.getDefinitionVersion(),
                submission.getSubmittedByUserId(),
                schemaEngine.readPayload(submission.getPayloadJson()),
                submission.getCreatedTaskId(),
                submission.getValidationContext(),
                submission.getSubmittedAt());
    }

    private String requiredText(Map<String, Object> values, String key, String role) {
        Object value = values.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Form " + role + " field must contain text");
        }
        return text.trim();
    }

    private String optionalText(Map<String, Object> values, String key) {
        if (key == null) {
            return null;
        }
        Object value = values.get(key);
        return value instanceof String text && !text.isBlank() ? text.trim() : null;
    }

    private java.time.Instant dueAt(Map<String, Object> values, String key) {
        if (key == null || !values.containsKey(key)) {
            return null;
        }
        Object value = values.get(key);
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Task due date mapping must contain an ISO date");
        }
        return LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Pageable bounded(Pageable pageable) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size);
    }
}
