package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public interface ApprovalReviewerEligibilityPort {
    boolean isEligible(UUID tenantId, UUID projectId, UUID userId);
}
