package com.chacha.multitenantsaas.whiteboards;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "whiteboard_nodes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_whiteboard_node_key",
                        columnNames = {"tenant_id", "project_id", "board_id", "node_key"}),
        indexes =
                @Index(
                        name = "idx_whiteboard_nodes_board",
                        columnList = "tenant_id,project_id,board_id,z_index,node_key"))
public class WhiteboardNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "node_key", nullable = false, length = 64)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    private WhiteboardNodeType nodeType;

    @Column(length = 4000)
    private String content;

    @Column(name = "linked_task_id")
    private UUID linkedTaskId;

    @Column(name = "position_x", nullable = false)
    private int positionX;

    @Column(name = "position_y", nullable = false)
    private int positionY;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "z_index", nullable = false)
    private int zIndex;

    protected WhiteboardNode() {}

    public WhiteboardNode(
            UUID tenantId,
            UUID projectId,
            UUID boardId,
            String nodeKey,
            WhiteboardNodeType nodeType,
            String content,
            UUID linkedTaskId,
            int positionX,
            int positionY,
            int width,
            int height,
            int zIndex) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.boardId = boardId;
        this.nodeKey = nodeKey;
        this.nodeType = nodeType;
        this.content = content;
        this.linkedTaskId = linkedTaskId;
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.zIndex = zIndex;
    }

    public void linkTask(UUID taskId) {
        if (linkedTaskId != null) {
            throw new IllegalStateException("Whiteboard node is already linked to a task");
        }
        linkedTaskId = taskId;
    }

    public UUID getId() {
        return id;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public WhiteboardNodeType getNodeType() {
        return nodeType;
    }

    public String getContent() {
        return content;
    }

    public UUID getLinkedTaskId() {
        return linkedTaskId;
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getZIndex() {
        return zIndex;
    }
}
