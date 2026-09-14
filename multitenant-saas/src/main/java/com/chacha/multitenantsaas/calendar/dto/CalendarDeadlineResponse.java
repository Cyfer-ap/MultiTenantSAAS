package com.chacha.multitenantsaas.calendar.dto;

import java.time.Instant;
import java.util.List;

public record CalendarDeadlineResponse(
        Instant generatedAt,
        Instant from,
        Instant to,
        int returnedCount,
        boolean truncated,
        List<CalendarDeadlineItemResponse> items) {}
