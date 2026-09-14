package com.chacha.multitenantsaas.mywork.dto;

public record MyWorkSummaryResponse(
        int totalOpen, int overdue, int dueSoon, int blocked, int inProgress) {}
