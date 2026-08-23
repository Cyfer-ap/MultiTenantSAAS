package com.chacha.multitenantsaas.exception;

import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.common.ApiErrorResponse;
import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, String> KNOWN_UNIQUE_CONSTRAINT_MESSAGES =
            Map.of(
                    "uk_tenant_slug",
                    "Tenant slug already exists.",
                    "uk_user_email_per_tenant",
                    "User email already exists for this tenant.",
                    "uk_system_admin_email",
                    "System admin email already exists.");

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                exception.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(SubscriptionRestrictionException.class)
    public ResponseEntity<ApiErrorResponse> handleSubscriptionRestrictionException(
            SubscriptionRestrictionException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("restriction", exception.getRestrictionType().name());

        if (exception.getAccessReason() != null) {
            details.put("accessReason", exception.getAccessReason().name());
        }

        if (exception.getResource() != null) {
            details.put("resource", exception.getResource());
        }

        if (exception.getUsed() != null) {
            details.put("used", exception.getUsed().toString());
        }

        if (exception.getLimit() != null) {
            details.put("limit", exception.getLimit().toString());
        }

        return buildResponse(
                HttpStatus.CONFLICT,
                ErrorCode.INVALID_REQUEST,
                exception.getMessage(),
                request,
                details);
    }

    @ExceptionHandler(BillingProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleBillingProviderException(
            BillingProviderException exception, HttpServletRequest request) {
        log.warn(
                "Billing provider request failed while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception);

        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                ErrorCode.BILLING_PROVIDER_ERROR,
                exception.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        String duplicateMessage = findKnownUniqueConstraintMessage(exception);

        if (duplicateMessage != null) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    duplicateMessage,
                    request,
                    null);
        }

        log.error(
                "Unhandled data-integrity violation while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request,
                null);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handlePessimisticLockingFailureException(
            PessimisticLockingFailureException exception, HttpServletRequest request) {
        log.warn(
                "Transient database concurrency conflict while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception);

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.TEMPORARY_DATABASE_CONFLICT,
                "A temporary database conflict prevented the request "
                        + "from completing. Please retry.",
                request,
                Map.of("retryable", true));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException exception, HttpServletRequest request) {
        ApiErrorResponse response =
                ApiErrorResponse.of(
                        exception.getMessage(),
                        ErrorCode.RATE_LIMITED,
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        request.getRequestURI(),
                        Map.of(
                                "scope",
                                exception.getScope(),
                                "retryAfterSeconds",
                                exception.getRetryAfterSeconds()));

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED,
                "Validation failed",
                request,
                fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                exception.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST,
                "Invalid request body. Please check the submitted values.",
                request,
                null);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailedException(
            AuthenticationFailedException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.AUTHENTICATION_FAILED,
                exception.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ErrorCode.ACCESS_DENIED,
                "Forbidden. You do not have permission to access this resource.",
                request,
                null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", exception.getName());
        details.put("rejectedValue", exception.getValue());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_PARAMETER,
                "Invalid request parameter: " + exception.getName(),
                request,
                details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception exception, HttpServletRequest request) {
        log.error(
                "Unhandled exception while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request,
                null);
    }

    private String findKnownUniqueConstraintMessage(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            String message = current.getMessage();

            if (message != null) {
                String normalizedMessage = message.toLowerCase(Locale.ROOT);

                for (Map.Entry<String, String> entry :
                        KNOWN_UNIQUE_CONSTRAINT_MESSAGES.entrySet()) {
                    if (normalizedMessage.contains(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }

            current = current.getCause();
        }

        return null;
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            Object details) {
        ApiErrorResponse response =
                ApiErrorResponse.of(
                        message, errorCode, status.value(), request.getRequestURI(), details);

        return ResponseEntity.status(status).body(response);
    }
}
