package com.chacha.multitenantsaas.mywork.dto;

import com.chacha.multitenantsaas.mywork.model.MyWorkAttention;
import java.time.Instant;
import java.util.UUID;

public record MyWorkItemResponse(
        UUID taskId,
        UUID projectId,
        String title,
        String projectName,
        String status,
        String priority,
        Instant dueAt,
        Instant updatedAt,
        MyWorkAttention attention,
        String targetUrl) {}
