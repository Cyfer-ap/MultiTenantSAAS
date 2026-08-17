package com.chacha.multitenantsaas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.chacha.multitenantsaas.config.PublicAuthRateLimitProperties;
import com.chacha.multitenantsaas.exception.RateLimitExceededException;
import com.chacha.multitenantsaas.observability.PublicAuthRateLimitMetrics;
import org.junit.jupiter.api.Test;

class PublicAuthIdentityRateLimiterTest {

    @Test
    void normalizesEmailAndBlocksRotatingIpStyleIdentityFlooding() {
        PublicAuthRateLimitProperties properties = new PublicAuthRateLimitProperties();
        properties.setEnabled(true);
        properties.setWindowSeconds(60L);
        properties.setRecoveryMaxRequests(2);

        PublicAuthRateLimitMetrics metrics = mock(PublicAuthRateLimitMetrics.class);
        PublicAuthIdentityRateLimiter limiter =
                new PublicAuthIdentityRateLimiter(properties, metrics, new SecureTokenService());

        limiter.checkWorkspaceDiscovery(" Grace@Example.com ");
        limiter.checkWorkspaceDiscovery("grace@example.com");

        RateLimitExceededException exception =
                assertThrows(
                        RateLimitExceededException.class,
                        () -> limiter.checkWorkspaceDiscovery("GRACE@EXAMPLE.COM"));

        assertEquals("recovery_identity", exception.getScope());
        verify(metrics).recordRejection("recovery_identity");
    }

    @Test
    void differentEmailsHaveIndependentIdentityBuckets() {
        PublicAuthRateLimitProperties properties = new PublicAuthRateLimitProperties();
        properties.setEnabled(true);
        properties.setWindowSeconds(60L);
        properties.setRecoveryMaxRequests(1);

        PublicAuthIdentityRateLimiter limiter =
                new PublicAuthIdentityRateLimiter(
                        properties,
                        mock(PublicAuthRateLimitMetrics.class),
                        new SecureTokenService());

        limiter.checkWorkspaceDiscovery("first@example.com");
        limiter.checkWorkspaceDiscovery("second@example.com");

        assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkWorkspaceDiscovery("first@example.com"));
    }

    @Test
    void disabledLimiterDoesNotRejectIdentityRequests() {
        PublicAuthRateLimitProperties properties = new PublicAuthRateLimitProperties();
        properties.setEnabled(false);
        properties.setRecoveryMaxRequests(1);

        PublicAuthIdentityRateLimiter limiter =
                new PublicAuthIdentityRateLimiter(
                        properties,
                        mock(PublicAuthRateLimitMetrics.class),
                        new SecureTokenService());

        limiter.checkWorkspaceDiscovery("same@example.com");
        limiter.checkWorkspaceDiscovery("same@example.com");
    }
}
