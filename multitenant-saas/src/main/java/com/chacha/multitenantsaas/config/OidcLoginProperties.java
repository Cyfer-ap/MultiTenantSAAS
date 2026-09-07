package com.chacha.multitenantsaas.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.identity-federation.oidc")
public class OidcLoginProperties {

    private String redirectUri = "http://localhost:8081/api/auth/oidc/callback";
    private long authorizationTransactionMinutes = 5L;

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public long getAuthorizationTransactionMinutes() {
        return authorizationTransactionMinutes;
    }

    public void setAuthorizationTransactionMinutes(long authorizationTransactionMinutes) {
        this.authorizationTransactionMinutes = authorizationTransactionMinutes;
    }

    public URI requireRedirectUri() {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("OIDC redirect URI must be configured");
        }

        URI uri;
        try {
            uri = URI.create(redirectUri.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "OIDC redirect URI must be a valid absolute URI", exception);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("OIDC redirect URI must include a host");
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException(
                    "OIDC redirect URI must not include credentials, query parameters, or a fragment");
        }

        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        boolean localHttp =
                "http".equalsIgnoreCase(uri.getScheme())
                        && isLoopbackDevelopmentHost(uri.getHost());
        if (!https && !localHttp) {
            throw new IllegalStateException(
                    "OIDC redirect URI must use HTTPS except for a loopback local-development URI");
        }

        return uri;
    }

    public Duration authorizationTransactionTtl() {
        if (authorizationTransactionMinutes < 1L || authorizationTransactionMinutes > 15L) {
            throw new IllegalStateException(
                    "OIDC authorization transaction lifetime must be between 1 and 15 minutes");
        }
        return Duration.ofMinutes(authorizationTransactionMinutes);
    }

    private boolean isLoopbackDevelopmentHost(String host) {
        String normalized = host.toLowerCase();
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized);
    }
}
