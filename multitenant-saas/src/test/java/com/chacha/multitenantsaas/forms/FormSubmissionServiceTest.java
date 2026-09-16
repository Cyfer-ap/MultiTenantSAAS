package com.chacha.multitenantsaas.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class FormSubmissionServiceTest {

    @Mock private FormDefinitionRepository definitionRepository;
    @Mock private FormSubmissionRepository submissionRepository;
    @Mock private CurrentActorService currentActorService;
    @Mock private TaskCreationPort taskCreationPort;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AppUser actor;
    @Mock private Jwt jwt;

    private FormSchemaEngine schemaEngine;
    private FormSubmissionService service;

    @BeforeEach
    void setUp() {
        schemaEngine = new FormSchemaEngine(new ObjectMapper());
        service =
                new FormSubmissionService(
                        definitionRepository,
                        submissionRepository,
                        currentActorService,
                        taskCreationPort,
                        schemaEngine,
                        eventPublisher);
    }

    @Test
    void acceptedSubmissionCreatesTaskThroughTaskOwnedPortAndStoresProvenance() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID formId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        FormDefinition definition =
                new FormDefinition(
                        tenantId,
                        projectId,
                        actorId,
                        "Incident intake",
                        "incident intake",
                        null,
                        "title",
                        "details",
                        null,
                        ProjectTaskPriority.HIGH,
                        null);
        definition.replaceFields(
                List.of(
                        new FormField(definition, "title", "Title", FormFieldType.TEXT, true, null, 0),
                        new FormField(
                                definition,
                                "details",
                                "Details",
                                FormFieldType.TEXTAREA,
                                false,
                                null,
                                1)));
        definition.activate();

        when(definitionRepository.findDetailed(tenantId, projectId, formId))
                .thenReturn(Optional.of(definition));
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(actor.getId()).thenReturn(actorId);
        when(taskCreationPort.createTask(any(TaskCreationCommand.class)))
                .thenReturn(new TaskCreationResult(taskId, Instant.now()));
        when(submissionRepository.saveAndFlush(any(FormSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FormDtos.SubmissionResponse response =
                service.submit(
                        tenantId,
                        projectId,
                        formId,
                        new FormDtos.SubmissionRequest(
                                Map.of("title", "  Investigate outage ", "details", "Logs attached")),
                        jwt);

        ArgumentCaptor<TaskCreationCommand> command =
                ArgumentCaptor.forClass(TaskCreationCommand.class);
        verify(taskCreationPort).createTask(command.capture());
        assertThat(command.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(command.getValue().projectId()).isEqualTo(projectId);
        assertThat(command.getValue().title()).isEqualTo("Investigate outage");
        assertThat(command.getValue().description()).isEqualTo("Logs attached");
        assertThat(command.getValue().priority()).isEqualTo(ProjectTaskPriority.HIGH);
        assertThat(response.createdTaskId()).isEqualTo(taskId);
        assertThat(response.validationContext()).contains("payloadValidated=true");
    }
}
