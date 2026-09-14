package com.chacha.multitenantsaas.calendar.dto;

import java.time.Instant;
import java.util.UUID;

public record CalendarDeadlineItemResponse(
        UUID taskId,
        UUID projectId,
        String title,
        String projectName,
        String status,
        String priority,
        Instant dueAt,
        String targetUrl) {}
