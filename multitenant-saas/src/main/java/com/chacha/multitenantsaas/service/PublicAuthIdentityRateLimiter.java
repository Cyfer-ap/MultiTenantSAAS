package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.PublicAuthRateLimitProperties;
import com.chacha.multitenantsaas.exception.RateLimitExceededException;
import com.chacha.multitenantsaas.observability.PublicAuthRateLimitMetrics;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class PublicAuthIdentityRateLimiter {

    private static final String RECOVERY_IDENTITY_SCOPE = "recovery_identity";
    private static final long CLEANUP_INTERVAL = 256L;

    private final PublicAuthRateLimitProperties properties;
    private final PublicAuthRateLimitMetrics rateLimitMetrics;
    private final SecureTokenService secureTokenService;
    private final ConcurrentMap<IdentityRateLimitKey, RateLimitWindow> windows =
            new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();

    public PublicAuthIdentityRateLimiter(
            PublicAuthRateLimitProperties properties,
            PublicAuthRateLimitMetrics rateLimitMetrics,
            SecureTokenService secureTokenService) {
        this.properties = properties;
        this.rateLimitMetrics = rateLimitMetrics;
        this.secureTokenService = secureTokenService;
    }

    public void checkWorkspaceDiscovery(String email) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        check(
                RECOVERY_IDENTITY_SCOPE,
                secureTokenService.hashToken(normalizedEmail),
                properties.getRecoveryMaxRequests());
    }

    private void check(String scope, String identityHash, int maxRequests) {
        long windowSeconds = properties.getWindowSeconds();

        if (windowSeconds <= 0L || maxRequests <= 0) {
            throw new IllegalStateException(
                    "Public authentication identity rate-limit configuration "
                            + "must use positive limits and window size.");
        }

        long nowEpochSecond = Instant.now().getEpochSecond();
        long windowStartEpochSecond = Math.floorDiv(nowEpochSecond, windowSeconds) * windowSeconds;

        if (requestSequence.incrementAndGet() % CLEANUP_INTERVAL == 0L) {
            removeExpiredWindows(windowStartEpochSecond);
        }

        IdentityRateLimitKey key = new IdentityRateLimitKey(scope, identityHash);

        RateLimitWindow updatedWindow =
                windows.compute(
                        key,
                        (ignored, currentWindow) -> {
                            if (currentWindow == null
                                    || currentWindow.windowStartEpochSecond()
                                            != windowStartEpochSecond) {
                                return new RateLimitWindow(windowStartEpochSecond, 1);
                            }

                            return new RateLimitWindow(
                                    windowStartEpochSecond, currentWindow.requestCount() + 1);
                        });

        if (updatedWindow.requestCount() > maxRequests) {
            long retryAfterSeconds =
                    Math.max(1L, windowStartEpochSecond + windowSeconds - nowEpochSecond);

            rateLimitMetrics.recordRejection(scope);
            throw new RateLimitExceededException(scope, retryAfterSeconds);
        }
    }

    private void removeExpiredWindows(long currentWindowStartEpochSecond) {
        windows.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue().windowStartEpochSecond()
                                        < currentWindowStartEpochSecond);
    }

    private record IdentityRateLimitKey(String scope, String identityHash) {}

    private record RateLimitWindow(long windowStartEpochSecond, int requestCount) {}
}
