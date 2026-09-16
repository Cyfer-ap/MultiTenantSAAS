package com.chacha.multitenantsaas.forms;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FormDtos {

    private FormDtos() {}

    public record FieldRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,63}$") String key,
            @NotBlank @Size(max = 120) String label,
            @NotNull FormFieldType type,
            boolean required,
            @NotNull @Size(max = 50) List<@NotBlank @Size(max = 100) String> options) {}

    public record UpsertRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @Size(max = 1000) String description,
            @NotNull @Size(min = 1, max = 30) List<@Valid FieldRequest> fields,
            @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,63}$")
                    String taskTitleFieldKey,
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,63}$") String taskDescriptionFieldKey,
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,63}$") String taskDueDateFieldKey,
            @NotNull ProjectTaskPriority taskPriority,
            UUID workflowId) {}

    public record FieldResponse(
            UUID id,
            String key,
            String label,
            FormFieldType type,
            boolean required,
            List<String> options,
            int position) {}

    public record SummaryResponse(
            UUID id,
            UUID tenantId,
            UUID projectId,
            String name,
            FormStatus status,
            int definitionVersion,
            UUID workflowId,
            Instant createdAt,
            Instant updatedAt) {}

    public record Response(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            String name,
            String description,
            FormStatus status,
            int definitionVersion,
            List<FieldResponse> fields,
            String taskTitleFieldKey,
            String taskDescriptionFieldKey,
            String taskDueDateFieldKey,
            ProjectTaskPriority taskPriority,
            UUID workflowId,
            Instant createdAt,
            Instant updatedAt) {}

    public record SubmissionRequest(@NotNull @Size(max = 30) Map<String, Object> values) {}

    public record SubmissionResponse(
            UUID id,
            UUID formId,
            int definitionVersion,
            UUID submittedByUserId,
            Map<String, Object> values,
            UUID createdTaskId,
            String validationContext,
            Instant submittedAt) {}
}
