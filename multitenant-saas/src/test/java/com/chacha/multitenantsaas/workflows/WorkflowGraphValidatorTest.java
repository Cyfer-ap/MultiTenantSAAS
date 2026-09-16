package com.chacha.multitenantsaas.workflows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkflowGraphValidatorTest {

    private final WorkflowGraphValidator validator = new WorkflowGraphValidator();

    @Test
    void acceptsBoundedTypedAcyclicGraphAndNormalizesConfiguration() {
        List<WorkflowDtos.NodeRequest> nodes =
                List.of(
                        node(
                                "trigger",
                                WorkflowNodeType.TRIGGER,
                                WorkflowOperation.TRIGGER_TASK_CREATED,
                                Map.of()),
                        node(
                                "priority",
                                WorkflowNodeType.CONDITION,
                                WorkflowOperation.CONDITION_TASK_PRIORITY_EQUALS,
                                Map.of("value", " urgent ")),
                        node(
                                "block",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_STATUS,
                                Map.of("value", "blocked")),
                        node(
                                "complete",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_STATUS,
                                Map.of("value", "completed")));
        List<WorkflowDtos.EdgeRequest> edges =
                List.of(
                        edge("trigger", "priority", WorkflowEdgeBranch.DEFAULT),
                        edge("priority", "block", WorkflowEdgeBranch.TRUE),
                        edge("priority", "complete", WorkflowEdgeBranch.FALSE));

        validator.validate(nodes, edges);

        assertThat(
                        validator.normalizeConfiguration(
                                WorkflowOperation.CONDITION_TASK_PRIORITY_EQUALS,
                                Map.of("value", " urgent ")))
                .containsEntry("value", "URGENT");
    }

    @Test
    void acceptsApprovalActionWithExplicitApprovedAndRejectedBranches() {
        String definitionId = UUID.randomUUID().toString();
        List<WorkflowDtos.NodeRequest> nodes =
                List.of(
                        node(
                                "trigger",
                                WorkflowNodeType.TRIGGER,
                                WorkflowOperation.TRIGGER_TASK_CREATED,
                                Map.of()),
                        node(
                                "approval",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_REQUEST_APPROVAL,
                                Map.of("value", definitionId)),
                        node(
                                "approved",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_STATUS,
                                Map.of("value", "IN_PROGRESS")),
                        node(
                                "rejected",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_STATUS,
                                Map.of("value", "BLOCKED")));
        List<WorkflowDtos.EdgeRequest> edges =
                List.of(
                        edge("trigger", "approval", WorkflowEdgeBranch.DEFAULT),
                        edge("approval", "approved", WorkflowEdgeBranch.APPROVED),
                        edge("approval", "rejected", WorkflowEdgeBranch.REJECTED));

        validator.validate(nodes, edges);

        assertThat(
                        validator.normalizeConfiguration(
                                WorkflowOperation.ACTION_REQUEST_APPROVAL,
                                Map.of("value", definitionId)))
                .containsEntry("value", definitionId);
    }

    @Test
    void rejectsApprovalActionWithoutBothOutcomeBranches() {
        String definitionId = UUID.randomUUID().toString();
        List<WorkflowDtos.NodeRequest> nodes =
                List.of(
                        node(
                                "trigger",
                                WorkflowNodeType.TRIGGER,
                                WorkflowOperation.TRIGGER_TASK_CREATED,
                                Map.of()),
                        node(
                                "approval",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_REQUEST_APPROVAL,
                                Map.of("value", definitionId)),
                        node(
                                "approved",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_STATUS,
                                Map.of("value", "IN_PROGRESS")));

        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        nodes,
                                        List.of(
                                                edge(
                                                        "trigger",
                                                        "approval",
                                                        WorkflowEdgeBranch.DEFAULT),
                                                edge(
                                                        "approval",
                                                        "approved",
                                                        WorkflowEdgeBranch.APPROVED))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APPROVED and REJECTED");
    }

    @Test
    void rejectsMalformedApprovalDefinitionId() {
        assertThatThrownBy(
                        () ->
                                validator.normalizeConfiguration(
                                        WorkflowOperation.ACTION_REQUEST_APPROVAL,
                                        Map.of("value", "not-a-uuid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid approval definition ID");
    }

    @Test
    void rejectsCycles() {
        List<WorkflowDtos.NodeRequest> nodes =
                List.of(
                        node(
                                "trigger",
                                WorkflowNodeType.TRIGGER,
                                WorkflowOperation.TRIGGER_TASK_CREATED,
                                Map.of()),
                        node(
                                "first",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_PRIORITY,
                                Map.of("value", "HIGH")),
                        node(
                                "second",
                                WorkflowNodeType.ACTION,
                                WorkflowOperation.ACTION_SET_TASK_PRIORITY,
                                Map.of("value", "LOW")));
        List<WorkflowDtos.EdgeRequest> edges =
                List.of(
                        edge("trigger", "first", WorkflowEdgeBranch.DEFAULT),
                        edge("first", "second", WorkflowEdgeBranch.DEFAULT),
                        edge("second", "first", WorkflowEdgeBranch.DEFAULT));

        assertThatThrownBy(() -> validator.validate(nodes, edges))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acyclic");
    }

    @Test
    void rejectsOperationTypeMismatchAndArbitraryConfiguration() {
        WorkflowDtos.NodeRequest invalid =
                node(
                        "trigger",
                        WorkflowNodeType.TRIGGER,
                        WorkflowOperation.ACTION_SET_TASK_STATUS,
                        Map.of("script", "rm -rf /"));

        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        List.of(
                                                invalid,
                                                node(
                                                        "action",
                                                        WorkflowNodeType.ACTION,
                                                        WorkflowOperation.ACTION_SET_TASK_STATUS,
                                                        Map.of("value", "TODO"))),
                                        List.of(
                                                edge(
                                                        "trigger",
                                                        "action",
                                                        WorkflowEdgeBranch.DEFAULT))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be used");
    }

    @Test
    void rejectsCancellationAsAStatusActionButAllowsItAsACondition() {
        assertThatThrownBy(
                        () ->
                                validator.normalizeConfiguration(
                                        WorkflowOperation.ACTION_SET_TASK_STATUS,
                                        Map.of("value", "cancelled")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot cancel tasks");

        assertThat(
                        validator.normalizeConfiguration(
                                WorkflowOperation.CONDITION_TASK_STATUS_EQUALS,
                                Map.of("value", "cancelled")))
                .containsEntry("value", "CANCELLED");
    }

    private WorkflowDtos.NodeRequest node(
            String key,
            WorkflowNodeType type,
            WorkflowOperation operation,
            Map<String, String> configuration) {
        return new WorkflowDtos.NodeRequest(key, type, operation, configuration, 100, 100);
    }

    private WorkflowDtos.EdgeRequest edge(String source, String target, WorkflowEdgeBranch branch) {
        return new WorkflowDtos.EdgeRequest(source, target, branch);
    }
}
