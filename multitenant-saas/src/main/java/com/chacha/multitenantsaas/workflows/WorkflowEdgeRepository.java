package com.chacha.multitenantsaas.workflows;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEdgeRepository extends JpaRepository<WorkflowEdge, UUID> {

    List<WorkflowEdge> findByTenantIdAndWorkflowIdOrderBySourceNodeKeyAscBranchTypeAsc(
            UUID tenantId, UUID workflowId);

    void deleteByTenantIdAndWorkflowId(UUID tenantId, UUID workflowId);
}
