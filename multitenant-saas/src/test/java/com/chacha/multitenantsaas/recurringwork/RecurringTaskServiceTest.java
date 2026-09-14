package com.chacha.multitenantsaas.recurringwork;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecurringTaskServiceTest {

    @Test
    void dailyAdvancePreservesLocalWallTimeAcrossDstBoundary() {
        ZoneId zone = ZoneId.of("America/New_York");
        Instant scheduled = ZonedDateTime.of(2026, 3, 7, 9, 0, 0, 0, zone).toInstant();
        RecurringTaskDefinition definition =
                new RecurringTaskDefinition(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "Daily check",
                        null,
                        ProjectTaskPriority.MEDIUM,
                        RecurrenceCadence.DAILY,
                        1,
                        zone.getId(),
                        scheduled,
                        null,
                        null,
                        null);

        Instant next = RecurringTaskService.advance(definition, scheduled);

        assertThat(next.atZone(zone).getHour()).isEqualTo(9);
        assertThat(next.atZone(zone).toLocalDate().toString()).isEqualTo("2026-03-08");
        assertThat(next).isEqualTo(Instant.parse("2026-03-08T13:00:00Z"));
    }

    @Test
    void monthlyAdvanceUsesCalendarMonthSemantics() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Instant scheduled = ZonedDateTime.of(2026, 1, 31, 10, 30, 0, 0, zone).toInstant();
        RecurringTaskDefinition definition =
                new RecurringTaskDefinition(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "Monthly close",
                        null,
                        ProjectTaskPriority.HIGH,
                        RecurrenceCadence.MONTHLY,
                        1,
                        zone.getId(),
                        scheduled,
                        null,
                        null,
                        null);

        Instant next = RecurringTaskService.advance(definition, scheduled);

        assertThat(next.atZone(zone).toLocalDate().toString()).isEqualTo("2026-02-28");
        assertThat(next.atZone(zone).toLocalTime().toString()).isEqualTo("10:30");
    }
}
