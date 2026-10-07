package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public interface ApprovalExternalGrantPort {

    ApprovalExternalGrantSnapshot requireApprovalReviewGrant(
            UUID tenantId, UUID projectId, UUID grantId);
}
