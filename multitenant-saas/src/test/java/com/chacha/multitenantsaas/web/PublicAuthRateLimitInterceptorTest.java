package com.chacha.multitenantsaas.web;

import com.chacha.multitenantsaas.config.PublicAuthRateLimitProperties;
import com.chacha.multitenantsaas.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicAuthRateLimitInterceptorTest {

    private PublicAuthRateLimitInterceptor interceptor;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        PublicAuthRateLimitProperties properties =
                new PublicAuthRateLimitProperties();
        properties.setEnabled(true);
        properties.setWindowSeconds(60L);
        properties.setLoginMaxRequests(2);
        properties.setRecoveryMaxRequests(1);
        properties.setTokenMaxRequests(1);
        properties.setOnboardingMaxRequests(1);

        interceptor = new PublicAuthRateLimitInterceptor(properties);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void blocksLoginAfterConfiguredLimit() {
        HttpServletRequest request = request(
                "/api/tenants/tenant-1/auth/login",
                "203.0.113.10"
        );

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertTrue(interceptor.preHandle(request, response, new Object()));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(
                        request,
                        response,
                        new Object()
                )
        );

        assertEquals("login", exception.getScope());
        assertTrue(exception.getRetryAfterSeconds() >= 1L);
        assertTrue(exception.getRetryAfterSeconds() <= 60L);
    }

    @Test
    void remoteAddressesHaveIndependentBuckets() {
        HttpServletRequest firstAddress = request(
                "/api/system/auth/login",
                "203.0.113.20"
        );
        HttpServletRequest secondAddress = request(
                "/api/system/auth/login",
                "203.0.113.21"
        );

        assertTrue(interceptor.preHandle(
                firstAddress,
                response,
                new Object()
        ));
        assertTrue(interceptor.preHandle(
                firstAddress,
                response,
                new Object()
        ));
        assertTrue(interceptor.preHandle(
                secondAddress,
                response,
                new Object()
        ));
    }

    @Test
    void recoveryRoutesShareOneBucketPerAddress() {
        HttpServletRequest forgotPassword = request(
                "/api/tenants/tenant-1/auth/forgot-password",
                "203.0.113.30"
        );
        HttpServletRequest resetPassword = request(
                "/api/auth/reset-password",
                "203.0.113.30"
        );

        assertTrue(interceptor.preHandle(
                forgotPassword,
                response,
                new Object()
        ));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(
                        resetPassword,
                        response,
                        new Object()
                )
        );

        assertEquals("recovery", exception.getScope());
    }

    @Test
    void refreshAndOnboardingUseIndependentScopes() {
        HttpServletRequest refresh = request(
                "/api/auth/refresh",
                "203.0.113.40"
        );
        HttpServletRequest onboarding = request(
                "/api/onboarding/tenants",
                "203.0.113.40"
        );

        assertTrue(interceptor.preHandle(refresh, response, new Object()));
        assertTrue(interceptor.preHandle(onboarding, response, new Object()));

        RateLimitExceededException refreshException = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(
                        refresh,
                        response,
                        new Object()
                )
        );
        RateLimitExceededException onboardingException = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(
                        onboarding,
                        response,
                        new Object()
                )
        );

        assertEquals("token", refreshException.getScope());
        assertEquals("onboarding", onboardingException.getScope());
    }

    @Test
    void logoutIsNotRateLimited() {
        HttpServletRequest logout = request(
                "/api/auth/logout",
                "203.0.113.50"
        );

        for (int attempt = 0; attempt < 20; attempt++) {
            assertTrue(interceptor.preHandle(
                    logout,
                    response,
                    new Object()
            ));
        }
    }

    private HttpServletRequest request(
            String path,
            String remoteAddress
    ) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn(path);
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn(remoteAddress);
        return request;
    }
}
