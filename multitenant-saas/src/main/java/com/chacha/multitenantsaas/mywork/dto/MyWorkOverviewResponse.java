package com.chacha.multitenantsaas.mywork.dto;

import java.time.Instant;
import java.util.List;

public record MyWorkOverviewResponse(
        Instant generatedAt,
        int dueSoonHours,
        MyWorkSummaryResponse summary,
        List<MyWorkItemResponse> items) {}
