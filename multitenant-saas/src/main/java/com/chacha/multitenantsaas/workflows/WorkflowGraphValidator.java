package com.chacha.multitenantsaas.workflows;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WorkflowGraphValidator {

    private static final int MAX_NODES = 50;
    private static final int MAX_EDGES = 100;

    public void validate(
            List<WorkflowDtos.NodeRequest> nodes, List<WorkflowDtos.EdgeRequest> edges) {
        if (nodes == null || nodes.size() < 2 || nodes.size() > MAX_NODES) {
            throw new IllegalArgumentException("A workflow must contain between 2 and 50 nodes");
        }
        if (edges == null || edges.isEmpty() || edges.size() > MAX_EDGES) {
            throw new IllegalArgumentException("A workflow must contain between 1 and 100 edges");
        }

        Map<String, WorkflowDtos.NodeRequest> nodesByKey = new LinkedHashMap<>();
        WorkflowDtos.NodeRequest trigger = null;
        for (WorkflowDtos.NodeRequest node : nodes) {
            if (node == null
                    || node.key() == null
                    || node.type() == null
                    || node.operation() == null) {
                throw new IllegalArgumentException("Every workflow node must be complete");
            }
            if (nodesByKey.putIfAbsent(node.key(), node) != null) {
                throw new IllegalArgumentException(
                        "Workflow node keys must be unique: " + node.key());
            }
            if (node.operation().nodeType() != node.type()) {
                throw new IllegalArgumentException(
                        "Operation "
                                + node.operation()
                                + " cannot be used as a "
                                + node.type()
                                + " node");
            }
            normalizeConfiguration(node.operation(), node.configuration());
            if (node.type() == WorkflowNodeType.TRIGGER) {
                if (trigger != null) {
                    throw new IllegalArgumentException(
                            "A workflow must contain exactly one trigger");
                }
                trigger = node;
            }
        }
        if (trigger == null) {
            throw new IllegalArgumentException("A workflow must contain exactly one trigger");
        }

        Map<String, List<WorkflowDtos.EdgeRequest>> outgoing = new HashMap<>();
        Map<String, Integer> incoming = new HashMap<>();
        Set<String> uniqueBranches = new HashSet<>();
        for (WorkflowDtos.EdgeRequest edge : edges) {
            if (edge == null
                    || !nodesByKey.containsKey(edge.sourceKey())
                    || !nodesByKey.containsKey(edge.targetKey())) {
                throw new IllegalArgumentException(
                        "Every workflow edge must reference existing nodes");
            }
            if (edge.sourceKey().equals(edge.targetKey())) {
                throw new IllegalArgumentException("Workflow edges cannot point to the same node");
            }
            String branchKey = edge.sourceKey() + "\u0000" + edge.branch();
            if (!uniqueBranches.add(branchKey)) {
                throw new IllegalArgumentException(
                        "A node cannot define the same outgoing branch more than once: "
                                + edge.sourceKey());
            }
            outgoing.computeIfAbsent(edge.sourceKey(), ignored -> new ArrayList<>()).add(edge);
            incoming.merge(edge.targetKey(), 1, Integer::sum);
        }

        if (incoming.getOrDefault(trigger.key(), 0) != 0) {
            throw new IllegalArgumentException("The trigger node cannot have incoming edges");
        }

        for (WorkflowDtos.NodeRequest node : nodes) {
            List<WorkflowDtos.EdgeRequest> nodeEdges = outgoing.getOrDefault(node.key(), List.of());
            switch (node.type()) {
                case TRIGGER -> validateLinearNode(node, nodeEdges, true);
                case ACTION -> validateLinearNode(node, nodeEdges, false);
                case CONDITION -> validateConditionNode(node, nodeEdges);
            }
        }

        ensureAcyclicAndReachable(trigger.key(), nodesByKey.keySet(), outgoing, incoming);
    }

    public Map<String, String> normalizeConfiguration(
            WorkflowOperation operation, Map<String, String> configuration) {
        Map<String, String> supplied = configuration == null ? Map.of() : configuration;
        if (operation.configurationKind() == WorkflowOperation.ConfigurationKind.NONE) {
            if (!supplied.isEmpty()) {
                throw new IllegalArgumentException(operation + " does not accept configuration");
            }
            return Map.of();
        }

        if (supplied.size() != 1 || !supplied.containsKey("value")) {
            throw new IllegalArgumentException(
                    operation + " requires exactly one configuration key: value");
        }
        String rawValue = supplied.get("value");
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(operation + " requires a non-blank value");
        }
        String value = rawValue.trim().toUpperCase(Locale.ROOT);

        try {
            if (operation.configurationKind() == WorkflowOperation.ConfigurationKind.PRIORITY) {
                ProjectTaskPriority.valueOf(value);
            } else if (operation.configurationKind()
                    == WorkflowOperation.ConfigurationKind.STATUS) {
                ProjectTaskStatus.valueOf(value);
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported value for " + operation + ": " + rawValue, exception);
        }

        if (operation == WorkflowOperation.ACTION_SET_TASK_STATUS
                && value.equals(ProjectTaskStatus.CANCELLED.name())) {
            throw new IllegalArgumentException(
                    "Workflow status actions cannot cancel tasks; cancellation uses the dedicated task lifecycle operation");
        }
        return Map.of("value", value);
    }

    private void validateLinearNode(
            WorkflowDtos.NodeRequest node,
            List<WorkflowDtos.EdgeRequest> edges,
            boolean requireOutgoing) {
        if ((requireOutgoing && edges.size() != 1) || (!requireOutgoing && edges.size() > 1)) {
            throw new IllegalArgumentException(
                    node.type()
                            + " node "
                            + node.key()
                            + " has an invalid number of outgoing edges");
        }
        if (edges.stream().anyMatch(edge -> edge.branch() != WorkflowEdgeBranch.DEFAULT)) {
            throw new IllegalArgumentException(
                    node.type() + " node " + node.key() + " may only use DEFAULT branches");
        }
    }

    private void validateConditionNode(
            WorkflowDtos.NodeRequest node, List<WorkflowDtos.EdgeRequest> edges) {
        if (edges.isEmpty() || edges.size() > 2) {
            throw new IllegalArgumentException(
                    "Condition node " + node.key() + " must define one or two branches");
        }
        if (edges.stream()
                .anyMatch(
                        edge ->
                                edge.branch() != WorkflowEdgeBranch.TRUE
                                        && edge.branch() != WorkflowEdgeBranch.FALSE)) {
            throw new IllegalArgumentException(
                    "Condition node " + node.key() + " may only use TRUE/FALSE branches");
        }
    }

    private void ensureAcyclicAndReachable(
            String triggerKey,
            Set<String> nodeKeys,
            Map<String, List<WorkflowDtos.EdgeRequest>> outgoing,
            Map<String, Integer> incoming) {
        Set<String> reachable = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(triggerKey);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!reachable.add(current)) {
                continue;
            }
            for (WorkflowDtos.EdgeRequest edge : outgoing.getOrDefault(current, List.of())) {
                queue.addLast(edge.targetKey());
            }
        }
        if (reachable.size() != nodeKeys.size()) {
            throw new IllegalArgumentException(
                    "Every workflow node must be reachable from the trigger");
        }

        Map<String, Integer> remainingIncoming = new HashMap<>();
        for (String key : nodeKeys) {
            remainingIncoming.put(key, incoming.getOrDefault(key, 0));
        }
        ArrayDeque<String> roots = new ArrayDeque<>();
        remainingIncoming.forEach(
                (key, count) -> {
                    if (count == 0) {
                        roots.addLast(key);
                    }
                });

        int visited = 0;
        while (!roots.isEmpty()) {
            String current = roots.removeFirst();
            visited++;
            for (WorkflowDtos.EdgeRequest edge : outgoing.getOrDefault(current, List.of())) {
                int next = remainingIncoming.merge(edge.targetKey(), -1, Integer::sum);
                if (next == 0) {
                    roots.addLast(edge.targetKey());
                }
            }
        }
        if (visited != nodeKeys.size()) {
            throw new IllegalArgumentException("Workflow graphs must be acyclic");
        }
    }
}
