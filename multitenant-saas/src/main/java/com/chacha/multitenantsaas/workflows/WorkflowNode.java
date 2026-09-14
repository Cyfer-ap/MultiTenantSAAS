package com.chacha.multitenantsaas.workflows;

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
        name = "workflow_nodes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_workflow_node_key",
                        columnNames = {"tenant_id", "workflow_id", "node_key"}),
        indexes =
                @Index(
                        name = "idx_workflow_nodes_definition",
                        columnList = "tenant_id,workflow_id,node_key"))
public class WorkflowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 64)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    private WorkflowNodeType nodeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 80)
    private WorkflowOperation operation;

    @Column(name = "configuration_json", length = 4000)
    private String configurationJson;

    @Column(name = "position_x", nullable = false)
    private int positionX;

    @Column(name = "position_y", nullable = false)
    private int positionY;

    protected WorkflowNode() {}

    public WorkflowNode(
            UUID tenantId,
            UUID workflowId,
            String nodeKey,
            WorkflowNodeType nodeType,
            WorkflowOperation operation,
            String configurationJson,
            int positionX,
            int positionY) {
        this.tenantId = tenantId;
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.nodeType = nodeType;
        this.operation = operation;
        this.configurationJson = configurationJson;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public UUID getId() {
        return id;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public WorkflowNodeType getNodeType() {
        return nodeType;
    }

    public WorkflowOperation getOperation() {
        return operation;
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }
}
