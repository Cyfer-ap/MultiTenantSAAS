package com.chacha.multitenantsaas.whiteboards;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WhiteboardDocumentValidator {

    static final int MAX_NODES = 300;
    static final int MAX_EDGES = 600;

    public void validate(
            List<WhiteboardDtos.NodeRequest> nodes, List<WhiteboardDtos.EdgeRequest> edges) {
        if (nodes == null || edges == null) {
            throw new IllegalArgumentException("Whiteboard nodes and edges are required");
        }
        if (nodes.size() > MAX_NODES) {
            throw new IllegalArgumentException("A whiteboard may contain at most 300 nodes");
        }
        if (edges.size() > MAX_EDGES) {
            throw new IllegalArgumentException("A whiteboard may contain at most 600 edges");
        }

        Set<String> nodeKeys = new HashSet<>();
        for (WhiteboardDtos.NodeRequest node : nodes) {
            if (!nodeKeys.add(node.key())) {
                throw new IllegalArgumentException("Duplicate whiteboard node key: " + node.key());
            }
        }

        Set<String> edgeKeys = new HashSet<>();
        for (WhiteboardDtos.EdgeRequest edge : edges) {
            if (edge.sourceKey().equals(edge.targetKey())) {
                throw new IllegalArgumentException("A whiteboard connector cannot target itself");
            }
            if (!nodeKeys.contains(edge.sourceKey()) || !nodeKeys.contains(edge.targetKey())) {
                throw new IllegalArgumentException("Whiteboard connectors must reference existing nodes");
            }
            String edgeKey = edge.sourceKey() + "\u0000" + edge.targetKey();
            if (!edgeKeys.add(edgeKey)) {
                throw new IllegalArgumentException(
                        "Duplicate whiteboard connector: "
                                + edge.sourceKey()
                                + " -> "
                                + edge.targetKey());
            }
        }
    }
}
