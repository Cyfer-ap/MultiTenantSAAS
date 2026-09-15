package com.chacha.multitenantsaas.whiteboards;

import com.chacha.multitenantsaas.common.ApiErrorResponse;
import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = WhiteboardController.class)
public class WhiteboardExceptionHandler {

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
}
