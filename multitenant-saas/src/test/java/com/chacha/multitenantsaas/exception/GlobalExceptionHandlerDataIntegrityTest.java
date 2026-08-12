package com.chacha.multitenantsaas.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.common.ApiErrorResponse;
import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerDataIntegrityTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void tenantSlugConstraintReturnsStableConflict() {
        assertConflict(
                handleConstraint(
                        "ERROR: duplicate key value violates unique constraint "
                                + "\"uk_tenant_slug\""),
                "Tenant slug already exists.");
    }

    @Test
    void tenantUserEmailConstraintIsCaseInsensitive() {
        assertConflict(
                handleConstraint(
                        "Unique index or primary key violation: "
                                + "\"PUBLIC.UK_USER_EMAIL_PER_TENANT\""),
                "User email already exists for this tenant.");
    }

    @Test
    void systemAdminEmailConstraintReturnsStableConflict() {
        assertConflict(
                handleConstraint(
                        "duplicate key value violates unique constraint "
                                + "\"uk_system_admin_email\""),
                "System admin email already exists.");
    }

    @Test
    void unknownIntegrityViolationRemainsInternalServerError() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "could not execute statement",
                        new RuntimeException(
                                "violates foreign key constraint " + "\"fk_project_member_user\""));

        ResponseEntity<ApiErrorResponse> response =
                exceptionHandler.handleDataIntegrityViolationException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, body.errorCode());
        assertEquals("An unexpected error occurred.", body.message());
    }

    private ResponseEntity<ApiErrorResponse> handleConstraint(String databaseMessage) {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "could not execute statement", new RuntimeException(databaseMessage));

        return exceptionHandler.handleDataIntegrityViolationException(exception, request);
    }

    private void assertConflict(ResponseEntity<ApiErrorResponse> response, String expectedMessage) {
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, body.errorCode());
        assertEquals(expectedMessage, body.message());
        assertEquals("/api/test", body.path());
    }
}
