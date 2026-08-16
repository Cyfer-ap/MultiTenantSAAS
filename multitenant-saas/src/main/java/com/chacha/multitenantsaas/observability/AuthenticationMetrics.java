package com.chacha.multitenantsaas.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationMetrics {

    public static final String LOGIN_ATTEMPTS_METRIC = "saas.security.login.attempts";
    public static final String ACCOUNT_LOCKS_METRIC = "saas.security.account.locks";

    private final MeterRegistry meterRegistry;

    public AuthenticationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLoginAttempt(AccountType accountType, LoginOutcome outcome) {
        Counter.builder(LOGIN_ATTEMPTS_METRIC)
                .description("Password login outcomes for SaaS accounts")
                .tag("account_type", accountType.tagValue)
                .tag("outcome", outcome.tagValue)
                .register(meterRegistry)
                .increment();
    }

    public void recordAccountLock(AccountType accountType) {
        Counter.builder(ACCOUNT_LOCKS_METRIC)
                .description("Accounts temporarily locked after repeated failed logins")
                .tag("account_type", accountType.tagValue)
                .register(meterRegistry)
                .increment();
    }

    public enum AccountType {
        TENANT_USER("tenant_user"),
        SYSTEM_ADMIN("system_admin");

        private final String tagValue;

        AccountType(String tagValue) {
            this.tagValue = tagValue;
        }
    }

    public enum LoginOutcome {
        SUCCESS("success"),
        FAILURE("failure"),
        BLOCKED("blocked");

        private final String tagValue;

        LoginOutcome(String tagValue) {
            this.tagValue = tagValue;
        }
    }
}
