package com.chacha.multitenantsaas.tasks.automation;

import java.util.UUID;

public interface TaskAutomationSnapshotPort {
    TaskAutomationSnapshot snapshot(UUID tenantId, UUID projectId, UUID taskId);
}
