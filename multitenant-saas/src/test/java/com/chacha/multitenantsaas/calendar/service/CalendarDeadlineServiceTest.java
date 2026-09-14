package com.chacha.multitenantsaas.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineContext;
import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineSnapshot;
import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineSource;
import com.chacha.multitenantsaas.dto.CurrentAuthorizationContextResponse;
import com.chacha.multitenantsaas.service.CurrentAuthorizationContextService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CalendarDeadlineServiceTest {

    @Test
    void returnsAuthorizedDeadlinesWithOwningTaskDeepLinks() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        Instant dueAt = Instant.parse("2026-09-20T08:00:00Z");
        Jwt jwt = mock(Jwt.class);
        CurrentAuthorizationContextService authorizationContextService =
                mock(CurrentAuthorizationContextService.class);
        CalendarDeadlineSource deadlineSource = mock(CalendarDeadlineSource.class);

        when(authorizationContextService.getCurrentAuthorizationContext(tenantId, jwt))
                .thenReturn(authorizationContext(tenantId, userId));
        when(deadlineSource.findDeadlines(
                        any(CalendarDeadlineContext.class), eq(from), eq(to), eq(26)))
                .thenReturn(
                        List.of(
                                new CalendarDeadlineSnapshot(
                                        taskId,
                                        projectId,
                                        "Prepare release",
                                        "Phoenix",
                                        "IN_PROGRESS",
                                        "HIGH",
                                        dueAt)));

        var service = new CalendarDeadlineService(authorizationContextService, deadlineSource);
        var response = service.getDeadlines(tenantId, from, to, 25, jwt);

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.returnedCount()).isEqualTo(1);
        assertThat(response.truncated()).isFalse();
        assertThat(response.items().getFirst().taskId()).isEqualTo(taskId);
        assertThat(response.items().getFirst().targetUrl())
                .isEqualTo("/projects/" + projectId + "?task=" + taskId);
    }

    @Test
    void reportsWhenAuthorizedResultsExceedTheRequestedLimit() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        Jwt jwt = mock(Jwt.class);
        CurrentAuthorizationContextService authorizationContextService =
                mock(CurrentAuthorizationContextService.class);
        CalendarDeadlineSource deadlineSource = mock(CalendarDeadlineSource.class);

        when(authorizationContextService.getCurrentAuthorizationContext(tenantId, jwt))
                .thenReturn(authorizationContext(tenantId, userId));
        when(deadlineSource.findDeadlines(
                        any(CalendarDeadlineContext.class), eq(from), eq(to), eq(2)))
                .thenReturn(
                        List.of(
                                new CalendarDeadlineSnapshot(
                                        UUID.randomUUID(),
                                        projectId,
                                        "First",
                                        "Phoenix",
                                        "TODO",
                                        "HIGH",
                                        Instant.parse("2026-09-10T08:00:00Z")),
                                new CalendarDeadlineSnapshot(
                                        UUID.randomUUID(),
                                        projectId,
                                        "Second",
                                        "Phoenix",
                                        "TODO",
                                        "LOW",
                                        Instant.parse("2026-09-11T08:00:00Z"))));

        var service = new CalendarDeadlineService(authorizationContextService, deadlineSource);
        var response = service.getDeadlines(tenantId, from, to, 1, jwt);

        assertThat(response.returnedCount()).isEqualTo(1);
        assertThat(response.truncated()).isTrue();
        assertThat(response.items()).extracting("title").containsExactly("First");
    }

    @Test
    void rejectsRangesLongerThanNinetyThreeDaysBeforeReadingAuthorization() {
        CurrentAuthorizationContextService authorizationContextService =
                mock(CurrentAuthorizationContextService.class);
        CalendarDeadlineSource deadlineSource = mock(CalendarDeadlineSource.class);
        var service = new CalendarDeadlineService(authorizationContextService, deadlineSource);

        assertThatThrownBy(
                        () ->
                                service.getDeadlines(
                                        UUID.randomUUID(),
                                        Instant.parse("2026-01-01T00:00:00Z"),
                                        Instant.parse("2026-05-01T00:00:00Z"),
                                        25,
                                        mock(Jwt.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("93 days");

        verifyNoInteractions(authorizationContextService, deadlineSource);
    }

    private CurrentAuthorizationContextResponse authorizationContext(UUID tenantId, UUID userId) {
        return new CurrentAuthorizationContextResponse(
                tenantId,
                userId,
                "Example User",
                "user@example.test",
                Instant.now(),
                List.of(),
                List.of(),
                List.of());
    }
}
