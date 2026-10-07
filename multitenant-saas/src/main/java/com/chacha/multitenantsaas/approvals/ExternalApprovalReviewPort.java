package com.chacha.multitenantsaas.approvals;

import java.util.List;
import java.util.UUID;

public interface ExternalApprovalReviewPort {

    List<ExternalApprovalReviewSummary> listPending(
            UUID tenantId, UUID projectId, UUID grantId, int limit);

    ExternalApprovalDecisionResult decide(ExternalApprovalDecisionCommand command);
}
