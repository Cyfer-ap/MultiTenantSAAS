package com.chacha.multitenantsaas.workflows;

import com.chacha.multitenantsaas.approvals.ApprovalResolvedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkflowApprovalResolutionListener {

    private final WorkflowRuntimeService runtimeService;

    public WorkflowApprovalResolutionListener(WorkflowRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResolved(ApprovalResolvedEvent event) {
        runtimeService.resumeApproval(event);
    }
}
