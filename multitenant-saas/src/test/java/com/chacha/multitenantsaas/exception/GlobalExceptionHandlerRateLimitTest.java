package com.chacha.multitenantsaas.exception;

import com.chacha.multitenantsaas.common.ApiErrorResponse;
import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerRateLimitTest {

    @Test
    void rateLimitReturnsStableTooManyRequestsContract() {
        GlobalExceptionHandler handler =
                new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRequestURI()).thenReturn(
                "/api/system/auth/login"
        );

        ResponseEntity<ApiErrorResponse> response =
                handler.handleRateLimitExceededException(
                        new RateLimitExceededException(
                                "login",
                                37L
                        ),
                        request
                );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                response.getStatusCode()
        );
        assertEquals(
                "37",
                response.getHeaders().getFirst(
                        HttpHeaders.RETRY_AFTER
                )
        );

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(ErrorCode.RATE_LIMITED, body.errorCode());
        assertEquals(429, body.status());
        assertEquals(
                "Too many requests. Please try again later.",
                body.message()
        );
        assertEquals(
                "/api/system/auth/login",
                body.path()
        );

        Map<?, ?> details = (Map<?, ?>) body.details();
        assertEquals("login", details.get("scope"));
        assertEquals(37L, details.get("retryAfterSeconds"));
    }
}
