package com.chacha.multitenantsaas.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class PublicAuthRateLimitMetricsTest {

    @Test
    void recordsRejectionByBoundedScope() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PublicAuthRateLimitMetrics metrics = new PublicAuthRateLimitMetrics(registry);

        metrics.recordRejection("recovery");

        Counter counter =
                registry.find(PublicAuthRateLimitMetrics.REJECTIONS_METRIC)
                        .tag("scope", "recovery")
                        .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
