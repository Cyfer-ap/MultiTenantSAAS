package com.chacha.multitenantsaas.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PublicAuthRateLimitMetrics {

    public static final String REJECTIONS_METRIC = "saas.security.rate_limit.rejections";

    private final MeterRegistry meterRegistry;

    public PublicAuthRateLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRejection(String scope) {
        Counter.builder(REJECTIONS_METRIC)
                .description("Public authentication requests rejected by rate limiting")
                .tag("scope", scope)
                .register(meterRegistry)
                .increment();
    }
}
