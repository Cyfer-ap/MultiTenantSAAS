package com.chacha.multitenantsaas.whiteboards;

import com.chacha.multitenantsaas.common.ApiErrorResponse;
import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = WhiteboardController.class)
public class WhiteboardExceptionHandler {

    private static final String WHITEBOARD_NAME_CONSTRAINT = "uk_whiteboard_name";

    @ExceptionHandler(WhiteboardVersionConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleVersionConflict(
            WhiteboardVersionConflictException exception, HttpServletRequest request) {
        ApiErrorResponse response =
                ApiErrorResponse.of(
                        exception.getMessage(),
                        ErrorCode.INVALID_REQUEST,
                        HttpStatus.CONFLICT.value(),
                        request.getRequestURI(),
                        Map.of(
                                "expectedVersion",
                                exception.getExpectedVersion(),
                                "currentVersion",
                                exception.getCurrentVersion()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        if (containsConstraint(exception, WHITEBOARD_NAME_CONSTRAINT)) {
            ApiErrorResponse response =
                    ApiErrorResponse.of(
                            "A whiteboard with this name already exists",
                            ErrorCode.RESOURCE_ALREADY_EXISTS,
                            HttpStatus.CONFLICT.value(),
                            request.getRequestURI(),
                            null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        ApiErrorResponse response =
                ApiErrorResponse.of(
                        "An unexpected error occurred.",
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        request.getRequestURI(),
                        null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private boolean containsConstraint(Throwable exception, String constraintName) {
        Throwable current = exception;
        String normalizedConstraint = constraintName.toLowerCase(Locale.ROOT);
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(normalizedConstraint)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
