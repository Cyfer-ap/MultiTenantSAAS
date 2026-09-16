package com.chacha.multitenantsaas.forms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class FormSchemaEngine {

    private static final int MAX_FIELDS = 30;
    private static final int MAX_OPTIONS = 50;
    private static final int MAX_PAYLOAD_LENGTH = 12000;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> PAYLOAD = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public FormSchemaEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateDefinition(FormDtos.UpsertRequest request) {
        if (request.fields() == null
                || request.fields().isEmpty()
                || request.fields().size() > MAX_FIELDS) {
            throw new IllegalArgumentException("A form must contain between 1 and 30 fields");
        }
        Set<String> keys = new LinkedHashSet<>();
        for (FormDtos.FieldRequest field : request.fields()) {
            if (field == null || !keys.add(field.key())) {
                throw new IllegalArgumentException("Form field keys must be unique");
            }
            normalizeOptions(field.type(), field.options());
        }
        validateMappings(
                request.taskTitleFieldKey(),
                request.taskDescriptionFieldKey(),
                request.taskDueDateFieldKey(),
                request.fields().stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        FormDtos.FieldRequest::key, field -> field)));
    }

    public void validateStored(FormDefinition definition) {
        if (definition.getFields().isEmpty() || definition.getFields().size() > MAX_FIELDS) {
            throw new IllegalStateException(
                    "Stored form field count is outside the supported bounds");
        }
        Map<String, FormField> byKey = new LinkedHashMap<>();
        for (FormField field : definition.getFields()) {
            if (byKey.putIfAbsent(field.getFieldKey(), field) != null) {
                throw new IllegalStateException("Stored form contains duplicate field keys");
            }
            normalizeOptions(field.getFieldType(), readOptions(field.getOptionsJson()));
        }
        FormField title = requireField(byKey, definition.getTaskTitleFieldKey(), "title");
        if (title.getFieldType() != FormFieldType.TEXT
                && title.getFieldType() != FormFieldType.SELECT) {
            throw new IllegalStateException("Task title must map to a text or select field");
        }
        if (definition.getTaskDescriptionFieldKey() != null) {
            FormField description =
                    requireField(byKey, definition.getTaskDescriptionFieldKey(), "description");
            if (description.getFieldType() != FormFieldType.TEXT
                    && description.getFieldType() != FormFieldType.TEXTAREA) {
                throw new IllegalStateException(
                        "Task description must map to a text or textarea field");
            }
        }
        if (definition.getTaskDueDateFieldKey() != null
                && requireField(byKey, definition.getTaskDueDateFieldKey(), "due date")
                                .getFieldType()
                        != FormFieldType.DATE) {
            throw new IllegalStateException("Task due date must map to a date field");
        }
    }

    public List<String> normalizeOptions(FormFieldType type, List<String> supplied) {
        List<String> options = supplied == null ? List.of() : supplied;
        if (type != FormFieldType.SELECT) {
            if (!options.isEmpty()) {
                throw new IllegalArgumentException(type + " fields cannot define select options");
            }
            return List.of();
        }
        if (options.isEmpty() || options.size() > MAX_OPTIONS) {
            throw new IllegalArgumentException(
                    "Select fields must define between 1 and 50 options");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String option : options) {
            if (option == null || option.isBlank() || option.trim().length() > 100) {
                throw new IllegalArgumentException(
                        "Select options must be non-blank and at most 100 characters");
            }
            if (!normalized.add(option.trim())) {
                throw new IllegalArgumentException("Select options must be unique");
            }
        }
        return List.copyOf(normalized);
    }

    public Map<String, Object> validateSubmission(
            FormDefinition definition, Map<String, Object> supplied) {
        Map<String, Object> values = supplied == null ? Map.of() : supplied;
        Map<String, FormField> fields = new LinkedHashMap<>();
        for (FormField field : definition.getFields()) {
            fields.put(field.getFieldKey(), field);
        }
        for (String key : values.keySet()) {
            if (!fields.containsKey(key)) {
                throw new IllegalArgumentException("Unknown form field: " + key);
            }
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        for (FormField field : definition.getFields()) {
            Object value = values.get(field.getFieldKey());
            if (isMissing(value)) {
                if (field.isRequired()) {
                    throw new IllegalArgumentException(
                            "Required form field is missing: " + field.getFieldKey());
                }
                continue;
            }
            normalized.put(field.getFieldKey(), normalizeValue(field, value));
        }
        String json = writePayload(normalized);
        if (json.length() > MAX_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("Form submission payload exceeds 12000 characters");
        }
        return normalized;
    }

    public String writeOptions(List<String> options) {
        return write(options);
    }

    public List<String> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored form options are invalid", exception);
        }
    }

    public String writePayload(Map<String, Object> payload) {
        return write(payload);
    }

    public Map<String, Object> readPayload(String json) {
        try {
            return objectMapper.readValue(json, PAYLOAD);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored form submission payload is invalid", exception);
        }
    }

    private Object normalizeValue(FormField field, Object value) {
        return switch (field.getFieldType()) {
            case TEXT -> normalizeText(value, field.getFieldKey(), 1000);
            case TEXTAREA -> normalizeText(value, field.getFieldKey(), 4000);
            case NUMBER -> normalizeNumber(value, field.getFieldKey());
            case DATE -> normalizeDate(value, field.getFieldKey());
            case BOOLEAN -> normalizeBoolean(value, field.getFieldKey());
            case SELECT -> normalizeSelect(field, value);
        };
    }

    private String normalizeText(Object value, String key, int maxLength) {
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Field " + key + " must be text");
        }
        String normalized = text.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("Field " + key + " exceeds its length limit");
        }
        return normalized;
    }

    private BigDecimal normalizeNumber(Object value, String key) {
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Field " + key + " must be a number", exception);
        }
    }

    private String normalizeDate(Object value, String key) {
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Field " + key + " must be an ISO date");
        }
        try {
            return LocalDate.parse(text.trim()).toString();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Field " + key + " must be an ISO date", exception);
        }
    }

    private Boolean normalizeBoolean(Object value, String key) {
        if (!(value instanceof Boolean bool)) {
            throw new IllegalArgumentException("Field " + key + " must be boolean");
        }
        return bool;
    }

    private String normalizeSelect(FormField field, Object value) {
        String selected = normalizeText(value, field.getFieldKey(), 100);
        if (!readOptions(field.getOptionsJson()).contains(selected)) {
            throw new IllegalArgumentException(
                    "Field " + field.getFieldKey() + " contains an unsupported option");
        }
        return selected;
    }

    private boolean isMissing(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }

    private FormField requireField(Map<String, FormField> fields, String key, String role) {
        FormField field = fields.get(key);
        if (field == null) {
            throw new IllegalStateException(
                    "Task " + role + " mapping references a missing form field");
        }
        return field;
    }

    private void validateMappings(
            String titleKey,
            String descriptionKey,
            String dueDateKey,
            Map<String, FormDtos.FieldRequest> fields) {
        FormDtos.FieldRequest title = fields.get(titleKey);
        if (title == null
                || (title.type() != FormFieldType.TEXT && title.type() != FormFieldType.SELECT)) {
            throw new IllegalArgumentException("Task title must map to a text or select field");
        }
        if (descriptionKey != null && !descriptionKey.isBlank()) {
            FormDtos.FieldRequest description = fields.get(descriptionKey);
            if (description == null
                    || (description.type() != FormFieldType.TEXT
                            && description.type() != FormFieldType.TEXTAREA)) {
                throw new IllegalArgumentException(
                        "Task description must map to a text or textarea field");
            }
        }
        if (dueDateKey != null && !dueDateKey.isBlank()) {
            FormDtos.FieldRequest dueDate = fields.get(dueDateKey);
            if (dueDate == null || dueDate.type() != FormFieldType.DATE) {
                throw new IllegalArgumentException("Task due date must map to a date field");
            }
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to encode form data", exception);
        }
    }
}
