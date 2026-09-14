package com.chacha.multitenantsaas.calendar.service;

import com.chacha.multitenantsaas.calendar.dto.CalendarDeadlineItemResponse;
import com.chacha.multitenantsaas.calendar.dto.CalendarDeadlineResponse;
import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineContext;
import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineSource;
import com.chacha.multitenantsaas.dto.CurrentAuthorizationContextResponse;
import com.chacha.multitenantsaas.service.CurrentAuthorizationContextService;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CalendarDeadlineService {

    private static final int DEFAULT_LIMIT = 250;
    private static final int MAX_LIMIT = 500;
    private static final Duration MAX_RANGE = Duration.ofDays(93);

    private final CurrentAuthorizationContextService authorizationContextService;
    private final CalendarDeadlineSource deadlineSource;

    public CalendarDeadlineService(
            CurrentAuthorizationContextService authorizationContextService,
            CalendarDeadlineSource deadlineSource) {
        this.authorizationContextService = authorizationContextService;
        this.deadlineSource = deadlineSource;
    }

    public CalendarDeadlineResponse getDeadlines(
            UUID tenantId, Instant from, Instant to, Integer requestedLimit, Jwt jwt) {
        validateRange(from, to);
        int limit = normalizeLimit(requestedLimit);

        CurrentAuthorizationContextResponse authorization =
                authorizationContextService.getCurrentAuthorizationContext(tenantId, jwt);
        CalendarDeadlineContext context =
                new CalendarDeadlineContext(
                        tenantId,
                        authorization.userId(),
                        authorization.grants().stream()
                                .map(
                                        grant ->
                                                new CalendarDeadlineContext.Grant(
                                                        grant.scopeType(),
                                                        grant.scopeTargetId(),
                                                        grant.permissionCodes()))
                                .toList());

        var candidates =
                deadlineSource.findDeadlines(context, from, to, limit + 1).stream()
                        .map(
                                deadline ->
                                        new CalendarDeadlineItemResponse(
                                                deadline.taskId(),
                                                deadline.projectId(),
                                                deadline.title(),
                                                deadline.projectName(),
                                                deadline.status(),
                                                deadline.priority(),
                                                deadline.dueAt(),
                                                "/projects/"
                                                        + deadline.projectId()
                                                        + "?task="
                                                        + deadline.taskId()))
                        .sorted(
                                Comparator.comparing(CalendarDeadlineItemResponse::dueAt)
                                        .thenComparing(
                                                CalendarDeadlineItemResponse::projectName,
                                                String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(
                                                CalendarDeadlineItemResponse::title,
                                                String.CASE_INSENSITIVE_ORDER))
                        .limit(limit + 1L)
                        .toList();

        boolean truncated = candidates.size() > limit;
        var items = candidates.subList(0, Math.min(limit, candidates.size()));
        return new CalendarDeadlineResponse(
                Instant.now(), from, to, items.size(), truncated, items);
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Calendar range requires both from and to timestamps.");
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("Calendar range end must be after its start.");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new IllegalArgumentException("Calendar range cannot exceed 93 days.");
        }
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }
}
