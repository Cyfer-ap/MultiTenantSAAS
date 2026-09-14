package com.chacha.multitenantsaas.workflows;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    boolean existsByTenantIdAndWorkflowIdAndEventKey(
            UUID tenantId, UUID workflowId, String eventKey);
}
