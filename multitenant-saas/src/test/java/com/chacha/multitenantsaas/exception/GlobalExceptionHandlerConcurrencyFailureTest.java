package com.chacha.multitenantsaas.exception;

import com.chacha.multitenantsaas.common.ApiErrorResponse;
import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerConcurrencyFailureTest {

    @Test
    void transientDatabaseConflictReturnsRetryableServiceUnavailable() {
        GlobalExceptionHandler handler =
                new GlobalExceptionHandler();

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn(
                "/api/tenants/test/projects"
        );

        ResponseEntity<ApiErrorResponse> response =
                handler.handlePessimisticLockingFailureException(
                        new PessimisticLockingFailureException(
                                "could not obtain database lock"
                        ),
                        request
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(
                ErrorCode.TEMPORARY_DATABASE_CONFLICT,
                body.errorCode()
        );
        assertEquals(503, body.status());
        assertEquals(
                "/api/tenants/test/projects",
                body.path()
        );
        assertEquals(
                "A temporary database conflict prevented the request "
                        + "from completing. Please retry.",
                body.message()
        );

        Map<?, ?> details = (Map<?, ?>) body.details();

        assertEquals(
                Boolean.TRUE,
                details.get("retryable")
        );
    }
}
