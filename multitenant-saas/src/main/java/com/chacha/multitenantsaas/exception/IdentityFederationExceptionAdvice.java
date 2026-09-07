package com.chacha.multitenantsaas.exception;

import com.chacha.multitenantsaas.common.ApiErrorResponse;
import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityFederationExceptionAdvice {

    @ExceptionHandler(IdentityFederationUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnavailable(
            IdentityFederationUnavailableException exception, HttpServletRequest request) {
        ApiErrorResponse response =
                ApiErrorResponse.of(
                        exception.getMessage(),
                        ErrorCode.IDENTITY_FEDERATION_UNAVAILABLE,
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        request.getRequestURI(),
                        null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
