package com.chacha.multitenantsaas.workflows;

import com.chacha.multitenantsaas.tasks.events.TaskDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkflowTaskEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTaskEventListener.class);

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowTaskEventListener(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskEvent(TaskDomainEvent event) {
        try {
            workflowRuntimeService.handle(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to dispatch workflow event {} for task {}",
                    event.eventId(),
                    event.taskId(),
                    exception);
        }
    }
}
