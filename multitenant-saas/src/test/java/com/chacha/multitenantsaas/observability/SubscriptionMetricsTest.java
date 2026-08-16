package com.chacha.multitenantsaas.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class SubscriptionMetricsTest {

    @Test
    void recordsSubscriptionRestrictionWithLowCardinalityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SubscriptionMetrics metrics = new SubscriptionMetrics(registry);

        metrics.recordRestriction(
                new SubscriptionRestrictionException(
                        RestrictionType.USER_LIMIT_REACHED,
                        SubscriptionAccessReason.ACTIVE,
                        "users",
                        5L,
                        5L,
                        "User limit reached."));

        Counter counter =
                registry.find(SubscriptionMetrics.RESTRICTIONS_METRIC)
                        .tags(
                                "restriction",
                                "USER_LIMIT_REACHED",
                                "resource",
                                "users",
                                "reason",
                                "ACTIVE")
                        .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
