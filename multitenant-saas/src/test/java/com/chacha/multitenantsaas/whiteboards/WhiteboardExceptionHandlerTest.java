package com.chacha.multitenantsaas.whiteboards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class WhiteboardExceptionHandlerTest {

    @Test
    void staleVersionReturnsConflictWithBothVersions() {
        WhiteboardExceptionHandler handler = new WhiteboardExceptionHandler();
        HttpServletRequest request = request();

        var response =
                handler.handleVersionConflict(
                        new WhiteboardVersionConflictException(4L, 5L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().details())
                .isEqualTo(Map.of("expectedVersion", 4L, "currentVersion", 5L));
    }

    @Test
    void duplicateNameConstraintRaceReturnsResourceConflict() {
        WhiteboardExceptionHandler handler = new WhiteboardExceptionHandler();
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "could not execute statement",
                        new RuntimeException(
                                "duplicate key value violates unique constraint \"uk_whiteboard_name\""));

        var response = handler.handleDataIntegrityViolation(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS);
        assertThat(response.getBody().message())
                .isEqualTo("A whiteboard with this name already exists");
    }

    @Test
    void unrelatedIntegrityViolationRemainsInternalError() {
        WhiteboardExceptionHandler handler = new WhiteboardExceptionHandler();
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "could not execute statement",
                        new RuntimeException("violates foreign key constraint \"fk_unknown\""));

        var response = handler.handleDataIntegrityViolation(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/whiteboards/board-id");
        return request;
    }
}
