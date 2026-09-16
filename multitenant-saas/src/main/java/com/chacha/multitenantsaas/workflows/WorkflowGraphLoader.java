package com.chacha.multitenantsaas.workflows;

import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowGraphLoader {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;

    public WorkflowGraphLoader(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowEdgeRepository edgeRepository) {
        this.definitionRepository = definitionRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinition> activeDefinitions(UUID tenantId) {
        return definitionRepository.findByTenantIdAndStatusOrderByNameAsc(
                tenantId, WorkflowStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public WorkflowDefinition requireDefinition(UUID tenantId, UUID workflowId) {
        return definitionRepository
                .findByTenantIdAndId(tenantId, workflowId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Workflow not found: " + workflowId));
    }

    @Transactional(readOnly = true)
    public List<WorkflowNode> nodes(WorkflowDefinition definition) {
        return nodeRepository.findByTenantIdAndWorkflowIdOrderByNodeKeyAsc(
                definition.getTenantId(), definition.getId());
    }

    @Transactional(readOnly = true)
    public List<WorkflowEdge> edges(WorkflowDefinition definition) {
        return edgeRepository.findByTenantIdAndWorkflowIdOrderBySourceNodeKeyAscBranchTypeAsc(
                definition.getTenantId(), definition.getId());
    }

    @Transactional(readOnly = true)
    public WorkflowNode trigger(WorkflowDefinition definition) {
        return nodes(definition).stream()
                .filter(node -> node.getNodeType() == WorkflowNodeType.TRIGGER)
                .findFirst()
                .orElse(null);
    }
}
