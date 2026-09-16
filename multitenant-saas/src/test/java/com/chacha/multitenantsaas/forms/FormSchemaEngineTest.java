package com.chacha.multitenantsaas.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class FormSchemaEngineTest {

    private final FormSchemaEngine engine = new FormSchemaEngine(new ObjectMapper());

    @Test
    void validatesAndNormalizesBoundedSubmission() {
        FormDefinition definition = definition();
        definition.replaceFields(
                List.of(
                        new FormField(definition, "title", "Title", FormFieldType.TEXT, true, null, 0),
                        new FormField(
                                definition,
                                "kind",
                                "Kind",
                                FormFieldType.SELECT,
                                true,
                                engine.writeOptions(List.of("Bug", "Feature")),
                                1),
                        new FormField(definition, "due", "Due", FormFieldType.DATE, false, null, 2)));

        engine.validateStored(definition);
        Map<String, Object> normalized =
                engine.validateSubmission(
                        definition,
                        Map.of("title", "  Fix login  ", "kind", "Bug", "due", "2026-09-30"));

        assertThat(normalized)
                .containsEntry("title", "Fix login")
                .containsEntry("kind", "Bug")
                .containsEntry("due", "2026-09-30");
    }

    @Test
    void rejectsUnknownFieldsAndUnsupportedSelectOptions() {
        FormDefinition definition = definition();
        definition.replaceFields(
                List.of(
                        new FormField(definition, "title", "Title", FormFieldType.TEXT, true, null, 0),
                        new FormField(
                                definition,
                                "kind",
                                "Kind",
                                FormFieldType.SELECT,
                                true,
                                engine.writeOptions(List.of("Bug", "Feature")),
                                1)));

        assertThatThrownBy(
                        () ->
                                engine.validateSubmission(
                                        definition, Map.of("title", "Task", "extra", "value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown form field");
        assertThatThrownBy(
                        () ->
                                engine.validateSubmission(
                                        definition, Map.of("title", "Task", "kind", "Incident")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported option");
    }

    @Test
    void definitionRejectsInvalidTaskMappingsAndDuplicateOptions() {
        FormDtos.UpsertRequest badMapping =
                new FormDtos.UpsertRequest(
                        "Incident intake",
                        null,
                        List.of(
                                new FormDtos.FieldRequest(
                                        "count", "Count", FormFieldType.NUMBER, true, List.of())),
                        "count",
                        null,
                        null,
                        ProjectTaskPriority.MEDIUM,
                        null);
        assertThatThrownBy(() -> engine.validateDefinition(badMapping))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task title");

        assertThatThrownBy(
                        () ->
                                engine.normalizeOptions(
                                        FormFieldType.SELECT, List.of("Bug", " Bug ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unique");
    }

    private FormDefinition definition() {
        return new FormDefinition(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Incident intake",
                "incident intake",
                null,
                "title",
                null,
                "due",
                ProjectTaskPriority.HIGH,
                null);
    }
}
