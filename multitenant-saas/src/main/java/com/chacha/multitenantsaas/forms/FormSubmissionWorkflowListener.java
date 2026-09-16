package com.chacha.multitenantsaas.forms;

import com.chacha.multitenantsaas.workflows.WorkflowFormSubmissionCommand;
import com.chacha.multitenantsaas.workflows.WorkflowFormSubmissionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FormSubmissionWorkflowListener {

    private static final Logger log = LoggerFactory.getLogger(FormSubmissionWorkflowListener.class);

    private final WorkflowFormSubmissionPort workflowPort;

    public FormSubmissionWorkflowListener(WorkflowFormSubmissionPort workflowPort) {
        this.workflowPort = workflowPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccepted(FormSubmissionAcceptedEvent event) {
        try {
            workflowPort.handleFormSubmission(
                    new WorkflowFormSubmissionCommand(
                            event.tenantId(),
                            event.projectId(),
                            event.workflowId(),
                            event.formId(),
                            event.submissionId(),
                            event.createdTaskId(),
                            event.actorUserId(),
                            event.taskPriority()));
        } catch (RuntimeException exception) {
            log.warn(
                    "Form submission {} committed but workflow {} entry failed",
                    event.submissionId(),
                    event.workflowId(),
                    exception);
        }
    }
}
