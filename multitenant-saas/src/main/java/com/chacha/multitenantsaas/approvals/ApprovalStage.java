package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "approval_stages")
public class ApprovalStage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "stage_key", nullable = false, length = 64)
    private String stageKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @Column(name = "allow_requester_approval", nullable = false)
    private boolean allowRequesterApproval;

    protected ApprovalStage() {}

    public ApprovalStage(
            UUID tenantId,
            UUID projectId,
            UUID definitionId,
            String stageKey,
            String name,
            int positionIndex,
            boolean allowRequesterApproval) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.definitionId = definitionId;
        this.stageKey = stageKey;
        this.name = name;
        this.positionIndex = positionIndex;
        this.allowRequesterApproval = allowRequesterApproval;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public UUID getDefinitionId() { return definitionId; }
    public String getStageKey() { return stageKey; }
    public String getName() { return name; }
    public int getPositionIndex() { return positionIndex; }
    public boolean isAllowRequesterApproval() { return allowRequesterApproval; }
}
