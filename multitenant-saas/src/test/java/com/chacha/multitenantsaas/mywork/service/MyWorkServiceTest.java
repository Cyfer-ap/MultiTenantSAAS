package com.chacha.multitenantsaas.mywork.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.mywork.model.MyWorkAttention;
import com.chacha.multitenantsaas.mywork.spi.MyWorkTaskSnapshot;
import com.chacha.multitenantsaas.mywork.spi.MyWorkTaskSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MyWorkServiceTest {

    @Test
    void classifiesAndOrdersAssignedWorkByAttention() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.now();
        MyWorkTaskSource source = mock(MyWorkTaskSource.class);
        AppUser actor = mock(AppUser.class);
        when(actor.getId()).thenReturn(userId);

        MyWorkTaskSnapshot assigned =
                snapshot(
                        projectId, "Assigned", "TODO", "LOW", null, now.minus(2, ChronoUnit.HOURS));
        MyWorkTaskSnapshot inProgress =
                snapshot(
                        projectId,
                        "In progress",
                        "IN_PROGRESS",
                        "MEDIUM",
                        null,
                        now.minus(1, ChronoUnit.HOURS));
        MyWorkTaskSnapshot dueSoon =
                snapshot(
                        projectId, "Due soon", "TODO", "HIGH", now.plus(12, ChronoUnit.HOURS), now);
        MyWorkTaskSnapshot blocked = snapshot(projectId, "Blocked", "BLOCKED", "URGENT", null, now);
        MyWorkTaskSnapshot overdue =
                snapshot(projectId, "Overdue", "TODO", "HIGH", now.minus(6, ChronoUnit.HOURS), now);

        when(source.findAssignedOpenTasks(eq(tenantId), eq(userId), anyInt()))
                .thenReturn(List.of(assigned, inProgress, dueSoon, blocked, overdue));

        var overview = new MyWorkService(source).getOverview(tenantId, actor, 20);

        assertThat(overview.items())
                .extracting(item -> item.attention())
                .containsExactly(
                        MyWorkAttention.OVERDUE,
                        MyWorkAttention.BLOCKED,
                        MyWorkAttention.DUE_SOON,
                        MyWorkAttention.IN_PROGRESS,
                        MyWorkAttention.ASSIGNED);
        assertThat(overview.summary().totalOpen()).isEqualTo(5);
        assertThat(overview.summary().overdue()).isEqualTo(1);
        assertThat(overview.summary().dueSoon()).isEqualTo(1);
        assertThat(overview.summary().blocked()).isEqualTo(1);
        assertThat(overview.summary().inProgress()).isEqualTo(1);
        assertThat(overview.dueSoonHours()).isEqualTo(72);
    }

    @Test
    void clampsClientLimitAndKeepsSourceFetchBounded() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MyWorkTaskSource source = mock(MyWorkTaskSource.class);
        AppUser actor = mock(AppUser.class);
        when(actor.getId()).thenReturn(userId);
        when(source.findAssignedOpenTasks(eq(tenantId), eq(userId), anyInt()))
                .thenReturn(List.of());

        new MyWorkService(source).getOverview(tenantId, actor, 1000);

        verify(source).findAssignedOpenTasks(tenantId, userId, 400);
    }

    private MyWorkTaskSnapshot snapshot(
            UUID projectId,
            String title,
            String status,
            String priority,
            Instant dueAt,
            Instant updatedAt) {
        return new MyWorkTaskSnapshot(
                UUID.randomUUID(),
                projectId,
                title,
                "Project Phoenix",
                status,
                priority,
                dueAt,
                updatedAt);
    }
}
