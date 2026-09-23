package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public interface ApprovalCheckpointPort {
    UUID openCheckpoint(ApprovalCheckpointCommand command);
}
