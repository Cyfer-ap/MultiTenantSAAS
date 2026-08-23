package com.chacha.multitenantsaas.exception;

import java.time.Instant;

public class ApiUsageLimitExceededException extends RuntimeException {

    private final String metricCode;
    private final long used;
    private final long limit;
    private final Instant resetAt;

    public ApiUsageLimitExceededException(
            String metricCode, long used, long limit, Instant resetAt) {
        super("API request quota exceeded for the current billing period");
        this.metricCode = metricCode;
        this.used = used;
        this.limit = limit;
        this.resetAt = resetAt;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public long getUsed() {
        return used;
    }

    public long getLimit() {
        return limit;
    }

    public Instant getResetAt() {
        return resetAt;
    }
}
