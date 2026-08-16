package com.chacha.multitenantsaas.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.observability.AuthenticationMetrics.AccountType;
import com.chacha.multitenantsaas.observability.AuthenticationMetrics.LoginOutcome;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class AuthenticationMetricsTest {

    @Test
    void recordsLoginOutcomeWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthenticationMetrics metrics = new AuthenticationMetrics(registry);

        metrics.recordLoginAttempt(AccountType.TENANT_USER, LoginOutcome.FAILURE);

        Counter counter =
                registry.find(AuthenticationMetrics.LOGIN_ATTEMPTS_METRIC)
                        .tags("account_type", "tenant_user", "outcome", "failure")
                        .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordsAccountLockWithBoundedAccountType() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthenticationMetrics metrics = new AuthenticationMetrics(registry);

        metrics.recordAccountLock(AccountType.SYSTEM_ADMIN);

        Counter counter =
                registry.find(AuthenticationMetrics.ACCOUNT_LOCKS_METRIC)
                        .tag("account_type", "system_admin")
                        .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
