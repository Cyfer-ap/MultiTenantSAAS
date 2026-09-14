package com.chacha.multitenantsaas.calendar.controller;

import com.chacha.multitenantsaas.calendar.dto.CalendarDeadlineResponse;
import com.chacha.multitenantsaas.calendar.service.CalendarDeadlineService;
import com.chacha.multitenantsaas.common.ApiResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/calendar")
public class CalendarDeadlineController {

    private final CalendarDeadlineService calendarDeadlineService;

    public CalendarDeadlineController(CalendarDeadlineService calendarDeadlineService) {
        this.calendarDeadlineService = calendarDeadlineService;
    }

    @GetMapping("/deadlines")
    public ResponseEntity<ApiResponse<CalendarDeadlineResponse>> getDeadlines(
            @PathVariable UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Calendar deadlines fetched successfully",
                                calendarDeadlineService.getDeadlines(
                                        tenantId, from, to, limit, jwt)));
    }
}
