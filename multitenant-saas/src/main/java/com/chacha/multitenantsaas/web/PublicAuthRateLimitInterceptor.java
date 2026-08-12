package com.chacha.multitenantsaas.web;

import com.chacha.multitenantsaas.config.PublicAuthRateLimitProperties;
import com.chacha.multitenantsaas.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PublicAuthRateLimitInterceptor implements HandlerInterceptor {

    private static final Pattern TENANT_LOGIN_PATTERN =
            Pattern.compile("^/api/tenants/[^/]+/auth/login/?$");
    private static final Pattern TENANT_FORGOT_PASSWORD_PATTERN =
            Pattern.compile("^/api/tenants/[^/]+/auth/forgot-password/?$");

    private static final long CLEANUP_INTERVAL = 256L;

    private final PublicAuthRateLimitProperties properties;
    private final ConcurrentMap<RateLimitKey, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();

    public PublicAuthRateLimitInterceptor(PublicAuthRateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        RateLimitScope scope = resolveScope(requestPath(request));

        if (scope == null) {
            return true;
        }

        long windowSeconds = properties.getWindowSeconds();
        int maxRequests = maxRequests(scope);

        if (windowSeconds <= 0L || maxRequests <= 0) {
            throw new IllegalStateException(
                    "Public authentication rate-limit configuration "
                            + "must use positive limits and window size.");
        }

        long nowEpochSecond = Instant.now().getEpochSecond();
        long windowStartEpochSecond = Math.floorDiv(nowEpochSecond, windowSeconds) * windowSeconds;

        if (requestSequence.incrementAndGet() % CLEANUP_INTERVAL == 0L) {
            removeExpiredWindows(windowStartEpochSecond);
        }

        String remoteAddress = request.getRemoteAddr();

        if (remoteAddress == null || remoteAddress.isBlank()) {
            remoteAddress = "unknown";
        }

        RateLimitKey key = new RateLimitKey(scope, remoteAddress);

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

            throw new RateLimitExceededException(
                    scope.name().toLowerCase(Locale.ROOT), retryAfterSeconds);
        }

        return true;
    }

    private RateLimitScope resolveScope(String path) {
        if (TENANT_LOGIN_PATTERN.matcher(path).matches()
                || "/api/system/auth/login".equals(path)
                || "/api/system/auth/login/".equals(path)) {
            return RateLimitScope.LOGIN;
        }

        if (TENANT_FORGOT_PASSWORD_PATTERN.matcher(path).matches()
                || "/api/auth/reset-password".equals(path)
                || "/api/auth/reset-password/".equals(path)
                || "/api/user-invitations/accept".equals(path)
                || "/api/user-invitations/accept/".equals(path)) {
            return RateLimitScope.RECOVERY;
        }

        if ("/api/auth/refresh".equals(path) || "/api/auth/refresh/".equals(path)) {
            return RateLimitScope.TOKEN;
        }

        if ("/api/onboarding/tenants".equals(path) || "/api/onboarding/tenants/".equals(path)) {
            return RateLimitScope.ONBOARDING;
        }

        return null;
    }

    private int maxRequests(RateLimitScope scope) {
        return switch (scope) {
            case LOGIN -> properties.getLoginMaxRequests();
            case RECOVERY -> properties.getRecoveryMaxRequests();
            case TOKEN -> properties.getTokenMaxRequests();
            case ONBOARDING -> properties.getOnboardingMaxRequests();
        };
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (requestUri == null) {
            return "";
        }

        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    private void removeExpiredWindows(long currentWindowStartEpochSecond) {
        windows.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue().windowStartEpochSecond()
                                        < currentWindowStartEpochSecond);
    }

    private enum RateLimitScope {
        LOGIN,
        RECOVERY,
        TOKEN,
        ONBOARDING
    }

    private record RateLimitKey(RateLimitScope scope, String remoteAddress) {}

    private record RateLimitWindow(long windowStartEpochSecond, int requestCount) {}
}
