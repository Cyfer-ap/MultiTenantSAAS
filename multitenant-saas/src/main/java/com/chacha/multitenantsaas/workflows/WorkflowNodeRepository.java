package com.chacha.multitenantsaas.workflows;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, UUID> {

    List<WorkflowNode> findByTenantIdAndWorkflowIdOrderByNodeKeyAsc(
            UUID tenantId, UUID workflowId);

    void deleteByTenantIdAndWorkflowId(UUID tenantId, UUID workflowId);
}
