package com.chacha.multitenantsaas.calendar.spi;

import java.time.Instant;
import java.util.List;

public interface CalendarDeadlineSource {

    List<CalendarDeadlineSnapshot> findDeadlines(
            CalendarDeadlineContext context, Instant from, Instant to, int limit);
}
