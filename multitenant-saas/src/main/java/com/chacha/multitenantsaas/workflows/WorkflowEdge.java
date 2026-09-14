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
        name = "workflow_edges",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_workflow_edge_branch",
                        columnNames = {
                            "tenant_id", "workflow_id", "source_node_key", "branch_type"
                        }),
        indexes =
                @Index(
                        name = "idx_workflow_edges_definition",
                        columnList = "tenant_id,workflow_id,source_node_key"))
public class WorkflowEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "source_node_key", nullable = false, length = 64)
    private String sourceNodeKey;

    @Column(name = "target_node_key", nullable = false, length = 64)
    private String targetNodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", nullable = false, length = 20)
    private WorkflowEdgeBranch branchType;

    protected WorkflowEdge() {}

    public WorkflowEdge(
            UUID tenantId,
            UUID workflowId,
            String sourceNodeKey,
            String targetNodeKey,
            WorkflowEdgeBranch branchType) {
        this.tenantId = tenantId;
        this.workflowId = workflowId;
        this.sourceNodeKey = sourceNodeKey;
        this.targetNodeKey = targetNodeKey;
        this.branchType = branchType;
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

    public WorkflowEdgeBranch getBranchType() {
        return branchType;
    }
}
