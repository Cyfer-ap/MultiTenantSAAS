package com.chacha.multitenantsaas.workflows;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.approvals.ApprovalCheckpointPort;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationMutationCommand;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationMutationPort;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationMutationType;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationSnapshot;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationSnapshotPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowFormSubmissionRuntimeTest {

    @Mock private WorkflowGraphLoader graphLoader;
    @Mock private WorkflowExecutionRecorder executionRecorder;
    @Mock private TaskAutomationMutationPort taskMutationPort;
    @Mock private TaskAutomationSnapshotPort taskSnapshotPort;
    @Mock private ApprovalCheckpointPort approvalCheckpointPort;
    @Mock private WorkflowDefinition definition;

    private WorkflowRuntimeService runtime;

    @BeforeEach
    void setUp() {
        runtime =
                new WorkflowRuntimeService(
                        graphLoader,
                        executionRecorder,
                        taskMutationPort,
                        taskSnapshotPort,
                        approvalCheckpointPort,
                        new ObjectMapper());
    }

    @Test
    void formSubmissionEntryExecutesOnlySelectedActiveFormTrigger() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        WorkflowNode trigger =
                new WorkflowNode(
                        tenantId,
                        workflowId,
                        "trigger",
                        WorkflowNodeType.TRIGGER,
                        WorkflowOperation.TRIGGER_FORM_SUBMITTED,
                        "{}",
                        0,
                        0);
        List<WorkflowNode> nodes =
                List.of(
                        trigger,
                        new WorkflowNode(
                                tenantId,
                                workflowId,
                                "action",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_STATUS,
                                "{\"value\":\"IN_PROGRESS\"}",
                                250,
                                0));
        List<WorkflowEdge> edges =
                List.of(
                        new WorkflowEdge(
                                tenantId,
                                workflowId,
                                "trigger",
                                "action",
                                WorkflowEdgeBranch.DEFAULT));

        when(definition.getTenantId()).thenReturn(tenantId);
        when(definition.getId()).thenReturn(workflowId);
        when(definition.getStatus()).thenReturn(WorkflowStatus.ACTIVE);
        when(graphLoader.requireDefinition(tenantId, workflowId)).thenReturn(definition);
        when(graphLoader.trigger(definition)).thenReturn(trigger);
        when(graphLoader.nodes(definition)).thenReturn(nodes);
        when(graphLoader.edges(definition)).thenReturn(edges);
        when(executionRecorder.start(
                        definition,
                        WorkflowOperation.TRIGGER_FORM_SUBMITTED,
                        "FORM_SUBMISSION:" + submissionId,
                        "FORM_SUBMISSION",
                        submissionId))
                .thenReturn(Optional.of(executionId));
        when(taskMutationPort.mutate(any(TaskAutomationMutationCommand.class)))
                .thenReturn(
                        new TaskAutomationSnapshot(
                                ProjectTaskStatus.IN_PROGRESS, ProjectTaskPriority.HIGH));

        runtime.handleFormSubmission(
                new WorkflowFormSubmissionCommand(
                        tenantId,
                        projectId,
                        workflowId,
                        UUID.randomUUID(),
                        submissionId,
                        taskId,
                        actorId,
                        ProjectTaskPriority.HIGH));

        verify(taskMutationPort)
                .mutate(
                        org.mockito.ArgumentMatchers.argThat(
                                command ->
                                        command.taskId().equals(taskId)
                                                && command.mutationType()
                                                        == TaskAutomationMutationType.SET_STATUS
                                                && command.actorUserId().equals(actorId)));
        verify(executionRecorder)
                .succeed(
                        eq(executionId),
                        org.mockito.ArgumentMatchers.argThat(
                                explanation -> explanation.contains("TRIGGER_FORM_SUBMITTED")));
    }

    @Test
    void formTargetValidationRejectsTaskTriggeredWorkflow() {
        UUID tenantId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        WorkflowNode trigger =
                new WorkflowNode(
                        tenantId,
                        workflowId,
                        "trigger",
                        WorkflowNodeType.TRIGGER,
                        WorkflowOperation.TRIGGER_TASK_CREATED,
                        "{}",
                        0,
                        0);
        when(graphLoader.requireDefinition(tenantId, workflowId)).thenReturn(definition);
        when(graphLoader.trigger(definition)).thenReturn(trigger);

        assertThatThrownBy(() -> runtime.requireFormSubmissionTarget(tenantId, workflowId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Form submitted");
    }
}
