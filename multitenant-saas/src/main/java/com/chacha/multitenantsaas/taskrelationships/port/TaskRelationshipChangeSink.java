package com.chacha.multitenantsaas.taskrelationships.port;

import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface TaskRelationshipChangeSink {

    void record(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            Jwt jwt,
            AuditAction auditAction,
            TaskActivityType activityType,
            String summary);
}
