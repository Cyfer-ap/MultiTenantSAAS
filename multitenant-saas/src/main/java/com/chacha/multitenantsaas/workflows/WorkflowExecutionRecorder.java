package com.chacha.multitenantsaas.workflows;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowExecutionRecorder {

    private final WorkflowExecutionRepository executionRepository;

    public WorkflowExecutionRecorder(WorkflowExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> start(
            WorkflowDefinition definition,
            WorkflowOperation triggerOperation,
            String eventKey,
            UUID sourceEntityId) {
        return start(definition, triggerOperation, eventKey, "PROJECT_TASK", sourceEntityId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> start(
            WorkflowDefinition definition,
            WorkflowOperation triggerOperation,
            String eventKey,
            String sourceEntityType,
            UUID sourceEntityId) {
        if (executionRepository.existsByTenantIdAndWorkflowIdAndEventKey(
                definition.getTenantId(), definition.getId(), eventKey)) {
            return Optional.empty();
        }
        WorkflowExecution execution =
                executionRepository.saveAndFlush(
                        new WorkflowExecution(
                                definition.getTenantId(),
                                definition.getId(),
                                definition.getDefinitionVersion(),
                                eventKey,
                                triggerOperation,
                                sourceEntityType,
                                sourceEntityId));
        return Optional.of(execution.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void awaitApproval(UUID executionId, String explanation) {
        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        execution.awaitApproval(explanation);
        executionRepository.save(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resume(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        execution.resume();
        executionRepository.save(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeed(UUID executionId, String explanation) {
        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        execution.succeed(explanation);
        executionRepository.save(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID executionId, String errorMessage) {
        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        execution.fail(errorMessage);
        executionRepository.save(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void skip(UUID executionId, String explanation) {
        WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
        execution.skip(explanation);
        executionRepository.save(execution);
    }
}
