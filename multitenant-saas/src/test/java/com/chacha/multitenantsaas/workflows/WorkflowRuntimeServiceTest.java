package com.chacha.multitenantsaas.workflows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.approvals.ApprovalCheckpointCommand;
import com.chacha.multitenantsaas.approvals.ApprovalCheckpointPort;
import com.chacha.multitenantsaas.approvals.ApprovalRequestStatus;
import com.chacha.multitenantsaas.approvals.ApprovalResolvedEvent;
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
    void pausesSameExecutionAtApprovalCheckpointWithoutMutatingTask() {
        UUID tenantId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        UUID approvalDefinitionId = UUID.randomUUID();
        UUID approvalRequestId = UUID.randomUUID();
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
        WorkflowNode approval =
                new WorkflowNode(
                        tenantId,
                        workflowId,
                        "approval",
                        WorkflowNodeType.ACTION,
                        WorkflowOperation.ACTION_REQUEST_APPROVAL,
                        "{\"value\":\"" + approvalDefinitionId + "\"}",
                        200,
                        0);
        List<WorkflowNode> nodes = List.of(trigger, approval);
        List<WorkflowEdge> edges =
                List.of(
                        new WorkflowEdge(
                                tenantId,
                                workflowId,
                                "trigger",
                                "approval",
                                WorkflowEdgeBranch.DEFAULT),
                        new WorkflowEdge(
                                tenantId,
                                workflowId,
                                "approval",
                                "approved_action",
                                WorkflowEdgeBranch.APPROVED),
                        new WorkflowEdge(
                                tenantId,
                                workflowId,
                                "approval",
                                "rejected_action",
                                WorkflowEdgeBranch.REJECTED));

        when(definition.getTenantId()).thenReturn(tenantId);
        when(definition.getId()).thenReturn(workflowId);
        when(definition.getDefinitionVersion()).thenReturn(4);
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
        when(approvalCheckpointPort.openCheckpoint(any(ApprovalCheckpointCommand.class)))
                .thenReturn(approvalRequestId);

        runtimeService.handle(
                TaskDomainEvent.created(
                        tenantId,
                        projectId,
                        taskId,
                        actorId,
                        ProjectTaskStatus.TODO,
                        ProjectTaskPriority.HIGH));

        verify(approvalCheckpointPort)
                .openCheckpoint(
                        org.mockito.ArgumentMatchers.argThat(
                                command ->
                                        approvalDefinitionId.equals(command.approvalDefinitionId())
                                                && executionId.equals(
                                                        command.workflowExecutionId())
                                                && "approval".equals(command.workflowNodeKey())
                                                && "approved_action".equals(
                                                        command.approvedNextNodeKey())
                                                && "rejected_action".equals(
                                                        command.rejectedNextNodeKey())));
        verify(executionRecorder)
                .awaitApproval(
                        eq(executionId),
                        org.mockito.ArgumentMatchers.argThat(
                                explanation -> explanation.contains(approvalRequestId.toString())));
        verify(taskMutationPort, never()).mutate(any());
        verify(executionRecorder, never()).succeed(eq(executionId), anyString());
    }

    @Test
    void resumesApprovedCheckpointThroughTaskOwnedMutationPort() {
        UUID tenantId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        WorkflowNode action =
                new WorkflowNode(
                        tenantId,
                        workflowId,
                        "approved_action",
                        WorkflowNodeType.ACTION,
                        WorkflowOperation.ACTION_SET_TASK_STATUS,
                        "{\"value\":\"IN_PROGRESS\"}",
                        400,
                        0);
        ApprovalResolvedEvent event =
                new ApprovalResolvedEvent(
                        requestId,
                        tenantId,
                        projectId,
                        workflowId,
                        4,
                        executionId,
                        "approval",
                        taskId,
                        actorId,
                        ApprovalRequestStatus.APPROVED,
                        "approved_action",
                        "rejected_action");

        when(graphLoader.requireDefinition(tenantId, workflowId)).thenReturn(definition);
        when(definition.getId()).thenReturn(workflowId);
        when(definition.getDefinitionVersion()).thenReturn(4);
        when(graphLoader.nodes(definition)).thenReturn(List.of(action));
        when(graphLoader.edges(definition)).thenReturn(List.of());
        when(taskSnapshotPort.snapshot(tenantId, projectId, taskId))
                .thenReturn(
                        new TaskAutomationSnapshot(
                                ProjectTaskStatus.TODO, ProjectTaskPriority.HIGH));
        when(taskMutationPort.mutate(any(TaskAutomationMutationCommand.class)))
                .thenReturn(
                        new TaskAutomationSnapshot(
                                ProjectTaskStatus.IN_PROGRESS, ProjectTaskPriority.HIGH));

        runtimeService.resumeApproval(event);

        verify(executionRecorder).resume(executionId);
        verify(taskMutationPort)
                .mutate(
                        org.mockito.ArgumentMatchers.argThat(
                                command ->
                                        executionId.equals(command.workflowExecutionId())
                                                && actorId.equals(command.actorUserId())
                                                && command.mutationType()
                                                        == TaskAutomationMutationType.SET_STATUS
                                                && "IN_PROGRESS".equals(command.value())));
        verify(executionRecorder)
                .succeed(
                        eq(executionId),
                        org.mockito.ArgumentMatchers.argThat(
                                explanation ->
                                        explanation.contains("resolved APPROVED")
                                                && explanation.contains(
                                                        "Applied ACTION_SET_TASK_STATUS")));
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
