package com.chacha.multitenantsaas.observability;

import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMetrics {

    public static final String RESTRICTIONS_METRIC = "saas.subscription.restrictions";

    private final MeterRegistry meterRegistry;

    public SubscriptionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRestriction(SubscriptionRestrictionException exception) {
        Counter.builder(RESTRICTIONS_METRIC)
                .description("Subscription restrictions that blocked tenant growth operations")
                .tag("restriction", exception.getRestrictionType().name())
                .tag("resource", tagValue(exception.getResource()))
                .tag(
                        "reason",
                        exception.getAccessReason() == null
                                ? "NONE"
                                : exception.getAccessReason().name())
                .register(meterRegistry)
                .increment();
    }

    private String tagValue(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
