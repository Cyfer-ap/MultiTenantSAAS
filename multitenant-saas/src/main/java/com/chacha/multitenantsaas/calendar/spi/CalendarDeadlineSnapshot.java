package com.chacha.multitenantsaas.calendar.spi;

import java.time.Instant;
import java.util.UUID;

public record CalendarDeadlineSnapshot(
        UUID taskId,
        UUID projectId,
        String title,
        String projectName,
        String status,
        String priority,
        Instant dueAt) {}
