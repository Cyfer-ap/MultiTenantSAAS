package com.chacha.multitenantsaas.workflows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.chacha.multitenantsaas.tasks.events.TaskDomainEvent;
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
class WorkflowRuntimeServiceTest {

    @Mock private WorkflowGraphLoader graphLoader;
    @Mock private WorkflowExecutionRecorder executionRecorder;
    @Mock private TaskAutomationMutationPort taskMutationPort;
    @Mock private TaskAutomationSnapshotPort taskSnapshotPort;
    @Mock private ApprovalCheckpointPort approvalCheckpointPort;
    @Mock private WorkflowDefinition definition;

    private WorkflowRuntimeService runtimeService;

    @BeforeEach
    void setUp() {
        runtimeService =
                new WorkflowRuntimeService(
                        graphLoader,
                        executionRecorder,
                        taskMutationPort,
                        taskSnapshotPort,
                        approvalCheckpointPort,
                        new ObjectMapper());
    }

    @Test
    void executesMatchingCreatedTriggerConditionAndAction() {
        UUID tenantId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
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
        List<WorkflowNode> nodes =
                List.of(
                        trigger,
                        new WorkflowNode(
                                tenantId,
                                workflowId,
                                "condition",
                                WorkflowNodeType.CONDITION,
                                WorkflowOperation.CONDITION_TASK_PRIORITY_EQUALS,
                                "{\"value\":\"URGENT\"}",
                                200,
                                0),
                        new WorkflowNode(
                                tenantId,
                                workflowId,
                                "action",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_STATUS,
                                "{\"value\":\"BLOCKED\"}",
                                400,
                                0));
        List<WorkflowEdge> edges =
                List.of(
                        new WorkflowEdge(
                                tenantId,
                                workflowId,
                                "trigger",
                                "condition",
                                WorkflowEdgeBranch.DEFAULT),
                        new WorkflowEdge(
                                tenantId,
                                workflowId,
                                "condition",
                                "action",
                                WorkflowEdgeBranch.TRUE));

        when(definition.getTenantId()).thenReturn(tenantId);
        when(definition.getId()).thenReturn(workflowId);
        when(graphLoader.activeDefinitions(tenantId)).thenReturn(List.of(definition));
        when(graphLoader.trigger(definition)).thenReturn(trigger);
        when(graphLoader.nodes(definition)).thenReturn(nodes);
        when(graphLoader.edges(definition)).thenReturn(edges);
        when(executionRecorder.start(
                        eq(definition),
                        eq(WorkflowOperation.TRIGGER_TASK_CREATED),
                        anyString(),
                        eq(taskId)))
                .thenReturn(Optional.of(executionId));
        when(taskMutationPort.mutate(any(TaskAutomationMutationCommand.class)))
                .thenReturn(
                        new TaskAutomationSnapshot(
                                ProjectTaskStatus.BLOCKED, ProjectTaskPriority.URGENT));

        runtimeService.handle(
                TaskDomainEvent.created(
                        tenantId,
                        projectId,
                        taskId,
                        actorId,
                        ProjectTaskStatus.TODO,
                        ProjectTaskPriority.URGENT));

        verify(taskMutationPort)
                .mutate(
                        org.mockito.ArgumentMatchers.argThat(
                                command ->
                                        command.mutationType()
                                                        == TaskAutomationMutationType.SET_STATUS
                                                && "BLOCKED".equals(command.value())
                                                && actorId.equals(command.actorUserId())));
        verify(executionRecorder)
                .succeed(
                        eq(executionId),
                        org.mockito.ArgumentMatchers.argThat(
                                explanation ->
                                        explanation.contains("Applied ACTION_SET_TASK_STATUS")));
    }

    @Test
    void ignoresWorkflowWhoseTriggerDoesNotMatchEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        WorkflowNode trigger =
                new WorkflowNode(
                        tenantId,
                        workflowId,
                        "trigger",
                        WorkflowNodeType.TRIGGER,
                        WorkflowOperation.TRIGGER_TASK_STATUS_CHANGED,
                        "{}",
                        0,
                        0);

        when(definition.getTenantId()).thenReturn(tenantId);
        when(definition.getId()).thenReturn(workflowId);
        when(graphLoader.activeDefinitions(tenantId)).thenReturn(List.of(definition));
        when(graphLoader.trigger(definition)).thenReturn(trigger);

        runtimeService.handle(
                TaskDomainEvent.created(
                        tenantId,
                        UUID.randomUUID(),
                        taskId,
                        UUID.randomUUID(),
                        ProjectTaskStatus.TODO,
                        ProjectTaskPriority.MEDIUM));

        verify(executionRecorder, never()).start(any(), any(), anyString(), any());
        verify(taskMutationPort, never()).mutate(any());
    }
}
