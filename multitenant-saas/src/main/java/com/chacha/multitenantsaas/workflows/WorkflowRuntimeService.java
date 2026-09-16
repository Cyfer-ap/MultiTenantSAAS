package com.chacha.multitenantsaas.workflows;

import com.chacha.multitenantsaas.approvals.ApprovalCheckpointCommand;
import com.chacha.multitenantsaas.approvals.ApprovalCheckpointPort;
import com.chacha.multitenantsaas.approvals.ApprovalRequestStatus;
import com.chacha.multitenantsaas.approvals.ApprovalResolvedEvent;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationMutationCommand;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationMutationPort;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationMutationType;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationSnapshot;
import com.chacha.multitenantsaas.tasks.automation.TaskAutomationSnapshotPort;
import com.chacha.multitenantsaas.tasks.events.TaskDomainEvent;
import com.chacha.multitenantsaas.tasks.events.TaskDomainEventType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class WorkflowRuntimeService implements WorkflowFormSubmissionPort {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRuntimeService.class);
    private static final int MAX_VISITED_NODES = 50;
    private static final TypeReference<Map<String, String>> CONFIGURATION_TYPE =
            new TypeReference<>() {};

    private final WorkflowGraphLoader graphLoader;
    private final WorkflowExecutionRecorder executionRecorder;
    private final TaskAutomationMutationPort taskMutationPort;
    private final TaskAutomationSnapshotPort taskSnapshotPort;
    private final ApprovalCheckpointPort approvalCheckpointPort;
    private final ObjectMapper objectMapper;

    public WorkflowRuntimeService(
            WorkflowGraphLoader graphLoader,
            WorkflowExecutionRecorder executionRecorder,
            TaskAutomationMutationPort taskMutationPort,
            TaskAutomationSnapshotPort taskSnapshotPort,
            ApprovalCheckpointPort approvalCheckpointPort,
            ObjectMapper objectMapper) {
        this.graphLoader = graphLoader;
        this.executionRecorder = executionRecorder;
        this.taskMutationPort = taskMutationPort;
        this.taskSnapshotPort = taskSnapshotPort;
        this.approvalCheckpointPort = approvalCheckpointPort;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public void handle(TaskDomainEvent event) {
        for (WorkflowDefinition definition : graphLoader.activeDefinitions(event.tenantId())) {
            executeTaskIfTriggered(definition, event);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireFormSubmissionTarget(UUID tenantId, UUID workflowId) {
        WorkflowDefinition definition = graphLoader.requireDefinition(tenantId, workflowId);
        WorkflowNode trigger = graphLoader.trigger(definition);
        if (trigger == null || trigger.getOperation() != WorkflowOperation.TRIGGER_FORM_SUBMITTED) {
            throw new IllegalArgumentException("Selected workflow must use the Form submitted trigger");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void handleFormSubmission(WorkflowFormSubmissionCommand command) {
        WorkflowDefinition definition =
                graphLoader.requireDefinition(command.tenantId(), command.workflowId());
        if (definition.getStatus() != WorkflowStatus.ACTIVE) {
            return;
        }
        WorkflowNode trigger = graphLoader.trigger(definition);
        if (trigger == null || trigger.getOperation() != WorkflowOperation.TRIGGER_FORM_SUBMITTED) {
            return;
        }
        TaskDomainEvent taskContext =
                new TaskDomainEvent(
                        command.submissionId(),
                        TaskDomainEventType.CREATED,
                        command.tenantId(),
                        command.projectId(),
                        command.taskId(),
                        command.actorUserId(),
                        null,
                        ProjectTaskStatus.TODO,
                        command.taskPriority(),
                        Instant.now());
        execute(
                definition,
                trigger,
                taskContext,
                "FORM_SUBMISSION:" + command.submissionId(),
                "FORM_SUBMISSION",
                command.submissionId(),
                false);
    }

    @Transactional(readOnly = true)
    public void resumeApproval(ApprovalResolvedEvent event) {
        try {
            WorkflowDefinition definition =
                    graphLoader.requireDefinition(event.tenantId(), event.workflowId());
            if (definition.getDefinitionVersion() != event.workflowVersion()) {
                executionRecorder.fail(
                        event.workflowExecutionId(),
                        "Workflow definition changed while approval was pending");
                return;
            }
            TaskAutomationSnapshot snapshot =
                    taskSnapshotPort.snapshot(event.tenantId(), event.projectId(), event.taskId());
            TaskDomainEvent taskContext =
                    new TaskDomainEvent(
                            event.requestId(),
                            TaskDomainEventType.CREATED,
                            event.tenantId(),
                            event.projectId(),
                            event.taskId(),
                            event.actorUserId(),
                            snapshot.status(),
                            snapshot.status(),
                            snapshot.priority(),
                            Instant.now());
            String nextKey =
                    event.outcome() == ApprovalRequestStatus.APPROVED
                            ? event.approvedNextNodeKey()
                            : event.rejectedNextNodeKey();
            executionRecorder.resume(event.workflowExecutionId());
            List<String> explanation = new ArrayList<>();
            explanation.add(
                    "Approval request " + event.requestId() + " resolved " + event.outcome());
            traverse(
                    definition,
                    event.workflowExecutionId(),
                    graphLoader.nodes(definition),
                    graphLoader.edges(definition),
                    taskContext,
                    nextKey,
                    snapshot,
                    explanation);
        } catch (RuntimeException exception) {
            log.warn(
                    "Workflow execution {} failed while resuming approval request {}",
                    event.workflowExecutionId(),
                    event.requestId(),
                    exception);
            executionRecorder.fail(event.workflowExecutionId(), safeMessage(exception));
        }
    }

    private void executeTaskIfTriggered(WorkflowDefinition definition, TaskDomainEvent event) {
        WorkflowNode trigger = graphLoader.trigger(definition);
        if (trigger == null || !matchesTaskTrigger(trigger.getOperation(), event.type())) {
            return;
        }
        execute(
                definition,
                trigger,
                event,
                event.eventId().toString(),
                "PROJECT_TASK",
                event.taskId(),
                true);
    }

    private void execute(
            WorkflowDefinition definition,
            WorkflowNode trigger,
            TaskDomainEvent event,
            String eventKey,
            String sourceEntityType,
            UUID sourceEntityId,
            boolean taskEvent) {
        Optional<UUID> executionId =
                taskEvent
                        ? executionRecorder.start(
                                definition, trigger.getOperation(), eventKey, sourceEntityId)
                        : executionRecorder.start(
                                definition,
                                trigger.getOperation(),
                                eventKey,
                                sourceEntityType,
                                sourceEntityId);
        if (executionId.isEmpty()) {
            return;
        }
        List<WorkflowNode> nodes = graphLoader.nodes(definition);
        List<WorkflowEdge> edges = graphLoader.edges(definition);
        Map<String, Map<WorkflowEdgeBranch, String>> outgoing = outgoing(edges);
        String currentKey = next(outgoing, trigger.getNodeKey(), WorkflowEdgeBranch.DEFAULT);
        TaskAutomationSnapshot snapshot =
                new TaskAutomationSnapshot(event.status(), event.priority());
        List<String> explanation = new ArrayList<>();
        explanation.add("Matched " + trigger.getOperation());
        try {
            traverse(
                    definition,
                    executionId.get(),
                    nodes,
                    edges,
                    event,
                    currentKey,
                    snapshot,
                    explanation);
        } catch (RuntimeException exception) {
            log.warn(
                    "Workflow {} execution {} failed for source {}",
                    definition.getId(),
                    executionId.get(),
                    sourceEntityId,
                    exception);
            executionRecorder.fail(executionId.get(), safeMessage(exception));
        }
    }

    private void traverse(
            WorkflowDefinition definition,
            UUID executionId,
            List<WorkflowNode> nodes,
            List<WorkflowEdge> edges,
            TaskDomainEvent event,
            String currentKey,
            TaskAutomationSnapshot initialSnapshot,
            List<String> explanation) {
        Map<String, WorkflowNode> nodesByKey = new HashMap<>();
        for (WorkflowNode node : nodes) {
            nodesByKey.put(node.getNodeKey(), node);
        }
        Map<String, Map<WorkflowEdgeBranch, String>> outgoing = outgoing(edges);
        TaskAutomationSnapshot snapshot = initialSnapshot;
        int visited = 0;

        while (currentKey != null) {
            if (++visited > MAX_VISITED_NODES) {
                throw new IllegalStateException("Workflow traversal exceeded the node safety limit");
            }
            WorkflowNode node = nodesByKey.get(currentKey);
            if (node == null) {
                throw new IllegalStateException("Workflow edge references missing node: " + currentKey);
            }
            if (node.getNodeType() == WorkflowNodeType.CONDITION) {
                boolean result = evaluateCondition(node, snapshot);
                explanation.add(node.getOperation() + "=" + result);
                currentKey =
                        next(
                                outgoing,
                                node.getNodeKey(),
                                result ? WorkflowEdgeBranch.TRUE : WorkflowEdgeBranch.FALSE);
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.ACTION) {
                if (node.getOperation() == WorkflowOperation.ACTION_REQUEST_APPROVAL) {
                    UUID approvalDefinitionId = UUID.fromString(configurationValue(node));
                    String approvedNext =
                            next(outgoing, node.getNodeKey(), WorkflowEdgeBranch.APPROVED);
                    String rejectedNext =
                            next(outgoing, node.getNodeKey(), WorkflowEdgeBranch.REJECTED);
                    UUID requestId =
                            approvalCheckpointPort.openCheckpoint(
                                    new ApprovalCheckpointCommand(
                                            event.tenantId(),
                                            event.projectId(),
                                            approvalDefinitionId,
                                            definition.getId(),
                                            definition.getDefinitionVersion(),
                                            executionId,
                                            node.getNodeKey(),
                                            event.taskId(),
                                            event.actorUserId(),
                                            approvedNext,
                                            rejectedNext));
                    explanation.add("Waiting for approval request " + requestId);
                    executionRecorder.awaitApproval(executionId, String.join("; ", explanation));
                    return;
                }
                snapshot = applyTaskAction(definition, executionId, node, event, snapshot);
                explanation.add("Applied " + node.getOperation());
                currentKey = next(outgoing, node.getNodeKey(), WorkflowEdgeBranch.DEFAULT);
                continue;
            }
            throw new IllegalStateException("Only the first node may be a trigger");
        }
        executionRecorder.succeed(executionId, String.join("; ", explanation));
    }

    private boolean evaluateCondition(WorkflowNode node, TaskAutomationSnapshot snapshot) {
        String value = configurationValue(node);
        return switch (node.getOperation()) {
            case CONDITION_TASK_PRIORITY_EQUALS ->
                    snapshot.priority() != null && snapshot.priority().name().equals(value);
            case CONDITION_TASK_STATUS_EQUALS ->
                    snapshot.status() != null && snapshot.status().name().equals(value);
            default ->
                    throw new IllegalStateException(
                            "Unsupported workflow condition: " + node.getOperation());
        };
    }

    private TaskAutomationSnapshot applyTaskAction(
            WorkflowDefinition definition,
            UUID executionId,
            WorkflowNode node,
            TaskDomainEvent event,
            TaskAutomationSnapshot snapshot) {
        TaskAutomationMutationType mutationType =
                switch (node.getOperation()) {
                    case ACTION_SET_TASK_PRIORITY -> TaskAutomationMutationType.SET_PRIORITY;
                    case ACTION_SET_TASK_STATUS -> TaskAutomationMutationType.SET_STATUS;
                    default ->
                            throw new IllegalStateException(
                                    "Unsupported task workflow action: " + node.getOperation());
                };
        TaskAutomationSnapshot updated =
                taskMutationPort.mutate(
                        new TaskAutomationMutationCommand(
                                event.tenantId(),
                                event.projectId(),
                                event.taskId(),
                                event.actorUserId(),
                                definition.getId(),
                                executionId,
                                mutationType,
                                configurationValue(node)));
        return updated == null ? snapshot : updated;
    }

    private Map<String, Map<WorkflowEdgeBranch, String>> outgoing(List<WorkflowEdge> edges) {
        Map<String, Map<WorkflowEdgeBranch, String>> outgoing = new HashMap<>();
        for (WorkflowEdge edge : edges) {
            outgoing.computeIfAbsent(edge.getSourceNodeKey(), ignored -> new HashMap<>())
                    .put(edge.getBranchType(), edge.getTargetNodeKey());
        }
        return outgoing;
    }

    private String configurationValue(WorkflowNode node) {
        Map<String, String> configuration = parseConfiguration(node.getConfigurationJson());
        String value = configuration.get("value");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Workflow node " + node.getNodeKey() + " has no configuration value");
        }
        return value;
    }

    private Map<String, String> parseConfiguration(String configurationJson) {
        if (configurationJson == null || configurationJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(configurationJson, CONFIGURATION_TYPE);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored workflow configuration is invalid", exception);
        }
    }

    private String next(
            Map<String, Map<WorkflowEdgeBranch, String>> outgoing,
            String sourceKey,
            WorkflowEdgeBranch branch) {
        return outgoing.getOrDefault(sourceKey, Map.of()).get(branch);
    }

    private boolean matchesTaskTrigger(WorkflowOperation operation, TaskDomainEventType eventType) {
        return (operation == WorkflowOperation.TRIGGER_TASK_CREATED
                        && eventType == TaskDomainEventType.CREATED)
                || (operation == WorkflowOperation.TRIGGER_TASK_STATUS_CHANGED
                        && eventType == TaskDomainEventType.STATUS_CHANGED);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
