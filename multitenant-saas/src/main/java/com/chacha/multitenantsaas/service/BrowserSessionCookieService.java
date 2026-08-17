package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.BrowserSessionProperties;
import com.chacha.multitenantsaas.config.EmailVerificationProperties;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.dto.TokenRefreshResponse;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyResponse;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class BrowserSessionCookieService {

    public static final String CSRF_HEADER = "X-CSRF-Token";

    private static final String REFRESH_COOKIE_NAME = "mt_refresh";
    private static final String TRUSTED_BROWSER_COOKIE_NAME = "mt_trusted_email";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";
    private static final String TRUSTED_BROWSER_COOKIE_PATH = "/api/auth/workspaces";

    private final BrowserSessionProperties properties;
    private final long persistentExpirationDays;
    private final long trustedBrowserDays;

    public BrowserSessionCookieService(
            BrowserSessionProperties properties,
            EmailVerificationProperties emailVerificationProperties,
            @Value("${app.refresh-token.persistent-expiration-days:30}")
                    long persistentExpirationDays) {
        this.properties = properties;
        this.persistentExpirationDays = persistentExpirationDays;
        this.trustedBrowserDays = emailVerificationProperties.getTrustedBrowserDays();

        String sameSite = properties.getSameSite();

        if (sameSite == null
                || (!sameSite.equalsIgnoreCase("Lax")
                        && !sameSite.equalsIgnoreCase("Strict")
                        && !sameSite.equalsIgnoreCase("None"))) {
            throw new IllegalStateException(
                    "Browser-session SameSite must be Lax, Strict, or None");
        }

        if (sameSite.equalsIgnoreCase("None") && !properties.isSecure()) {
            throw new IllegalStateException(
                    "SameSite=None requires Secure browser-session cookies");
        }

        if (properties.isPartitioned() && !properties.isSecure()) {
            throw new IllegalStateException("Partitioned browser-session cookies require Secure");
        }
    }

    public boolean isCookieMode() {
        return properties.isCookieMode();
    }

    public String resolveRefreshToken(HttpServletRequest request, String legacyRefreshToken) {
        if (!isCookieMode()) {
            return requireValue(legacyRefreshToken);
        }

        return requireValue(readCookie(request, REFRESH_COOKIE_NAME));
    }

    public String resolveCsrfToken(HttpServletRequest request) {
        if (!isCookieMode()) {
            return null;
        }

        return requireValue(request.getHeader(CSRF_HEADER));
    }

    public String resolveTrustedBrowserToken(
            HttpServletRequest request, String legacyTrustedBrowserToken) {
        if (!isCookieMode()) {
            return legacyTrustedBrowserToken;
        }

        return readCookie(request, TRUSTED_BROWSER_COOKIE_NAME);
    }

    public LoginResponse applyLoginSession(
            HttpServletResponse servletResponse, LoginResponse response) {
        if (!isCookieMode()) {
            return response;
        }

        writeRefreshCookie(servletResponse, response.refreshToken(), response.persistentSession());
        noStore(servletResponse);

        return new LoginResponse(
                response.tenantId(),
                response.userId(),
                response.fullName(),
                response.email(),
                response.role(),
                response.accessToken(),
                null,
                response.csrfToken(),
                response.tokenType(),
                response.expiresInSeconds(),
                response.persistentSession(),
                response.message());
    }

    public TokenRefreshResponse applyRefreshSession(
            HttpServletResponse servletResponse, TokenRefreshResponse response) {
        if (!isCookieMode()) {
            return response;
        }

        writeRefreshCookie(servletResponse, response.refreshToken(), response.persistentSession());
        noStore(servletResponse);

        return new TokenRefreshResponse(
                response.accessToken(),
                null,
                response.csrfToken(),
                response.tokenType(),
                response.expiresInSeconds(),
                response.persistentSession(),
                response.message());
    }

    public WorkspaceDiscoveryVerifyResponse applyTrustedBrowserSession(
            HttpServletResponse servletResponse, WorkspaceDiscoveryVerifyResponse response) {
        if (!isCookieMode() || response.trustedBrowserToken() == null) {
            return response;
        }

        ResponseCookie trustedCookie =
                cookieBuilder(
                                TRUSTED_BROWSER_COOKIE_NAME,
                                response.trustedBrowserToken(),
                                TRUSTED_BROWSER_COOKIE_PATH)
                        .maxAge(Math.multiplyExact(trustedBrowserDays, 86_400L))
                        .build();

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, trustedCookie.toString());
        noStore(servletResponse);

        return new WorkspaceDiscoveryVerifyResponse(
                response.workspaces(), response.workspaceGrantId(), null, response.message());
    }

    public void clearRefreshCookie(HttpServletResponse servletResponse) {
        if (!isCookieMode()) {
            return;
        }

        ResponseCookie clearedCookie =
                cookieBuilder(REFRESH_COOKIE_NAME, "", REFRESH_COOKIE_PATH).maxAge(0).build();

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, clearedCookie.toString());
        noStore(servletResponse);
    }

    private void writeRefreshCookie(
            HttpServletResponse servletResponse,
            String rawRefreshToken,
            boolean persistentSession) {
        ResponseCookie.ResponseCookieBuilder builder =
                cookieBuilder(
                        REFRESH_COOKIE_NAME, requireValue(rawRefreshToken), REFRESH_COOKIE_PATH);

        if (persistentSession) {
            builder.maxAge(Math.multiplyExact(persistentExpirationDays, 86_400L));
        } else {
            builder.maxAge(-1);
        }

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(
            String name, String value, String path) {
        return ResponseCookie.from(name)
                .value(value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .partitioned(properties.isPartitioned())
                .sameSite(properties.getSameSite())
                .path(path);
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String requireValue(String value) {
        if (value == null || value.isBlank()) {
            throw new AuthenticationFailedException("Invalid session credentials");
        }

        return value.trim();
    }

    private void noStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
    }
}
