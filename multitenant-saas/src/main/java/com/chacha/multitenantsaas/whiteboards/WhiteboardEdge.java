package com.chacha.multitenantsaas.whiteboards;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "whiteboard_edges",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_whiteboard_edge",
                        columnNames = {
                            "tenant_id",
                            "project_id",
                            "board_id",
                            "source_node_key",
                            "target_node_key"
                        }),
        indexes =
                @Index(
                        name = "idx_whiteboard_edges_board",
                        columnList = "tenant_id,project_id,board_id,source_node_key"))
public class WhiteboardEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "source_node_key", nullable = false, length = 64)
    private String sourceNodeKey;

    @Column(name = "target_node_key", nullable = false, length = 64)
    private String targetNodeKey;

    @Column(length = 500)
    private String label;

    protected WhiteboardEdge() {}

    public WhiteboardEdge(
            UUID tenantId,
            UUID projectId,
            UUID boardId,
            String sourceNodeKey,
            String targetNodeKey,
            String label) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.boardId = boardId;
        this.sourceNodeKey = sourceNodeKey;
        this.targetNodeKey = targetNodeKey;
        this.label = label;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceNodeKey() {
        return sourceNodeKey;
    }

    public String getTargetNodeKey() {
        return targetNodeKey;
    }

    public String getLabel() {
        return label;
    }
}
