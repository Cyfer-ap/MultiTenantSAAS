package com.chacha.multitenantsaas.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.identity-federation.oidc")
public class OidcLoginProperties {

    private String redirectUri = "http://localhost:8081/api/auth/oidc/callback";
    private String frontendCompletionUri = "http://localhost:8080/auth/oidc/complete";
    private long authorizationTransactionMinutes = 5L;
    private long sessionHandoffMinutes = 2L;

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getFrontendCompletionUri() {
        return frontendCompletionUri;
    }

    public void setFrontendCompletionUri(String frontendCompletionUri) {
        this.frontendCompletionUri = frontendCompletionUri;
    }

    public long getAuthorizationTransactionMinutes() {
        return authorizationTransactionMinutes;
    }

    public void setAuthorizationTransactionMinutes(long authorizationTransactionMinutes) {
        this.authorizationTransactionMinutes = authorizationTransactionMinutes;
    }

    public long getSessionHandoffMinutes() {
        return sessionHandoffMinutes;
    }

    public void setSessionHandoffMinutes(long sessionHandoffMinutes) {
        this.sessionHandoffMinutes = sessionHandoffMinutes;
    }

    public URI requireRedirectUri() {
        return requireBrowserUri(redirectUri, "OIDC redirect URI");
    }

    public URI requireFrontendCompletionUri() {
        return requireBrowserUri(frontendCompletionUri, "OIDC frontend completion URI");
    }

    public Duration authorizationTransactionTtl() {
        if (authorizationTransactionMinutes < 1L || authorizationTransactionMinutes > 15L) {
            throw new IllegalStateException(
                    "OIDC authorization transaction lifetime must be between 1 and 15 minutes");
        }
        return Duration.ofMinutes(authorizationTransactionMinutes);
    }

    public Duration sessionHandoffTtl() {
        if (sessionHandoffMinutes < 1L || sessionHandoffMinutes > 5L) {
            throw new IllegalStateException(
                    "OIDC session handoff lifetime must be between 1 and 5 minutes");
        }
        return Duration.ofMinutes(sessionHandoffMinutes);
    }

    private URI requireBrowserUri(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(label + " must be configured");
        }

        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(label + " must be a valid absolute URI", exception);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException(label + " must include a host");
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException(
                    label + " must not include credentials, query parameters, or a fragment");
        }

        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        boolean localHttp =
                "http".equalsIgnoreCase(uri.getScheme())
                        && isLoopbackDevelopmentHost(uri.getHost());
        if (!https && !localHttp) {
            throw new IllegalStateException(
                    label + " must use HTTPS except for a loopback local-development URI");
        }

        return uri;
    }

    private boolean isLoopbackDevelopmentHost(String host) {
        String normalized = host.toLowerCase();
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized);
    }
}
