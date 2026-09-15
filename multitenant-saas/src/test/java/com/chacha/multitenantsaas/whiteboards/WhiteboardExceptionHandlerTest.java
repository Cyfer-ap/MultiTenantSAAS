package com.chacha.multitenantsaas.whiteboards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WhiteboardExceptionHandlerTest {

    @Test
    void staleVersionReturnsConflictWithBothVersions() {
        WhiteboardExceptionHandler handler = new WhiteboardExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/whiteboards/board-id");

        var response =
                handler.handleVersionConflict(
                        new WhiteboardVersionConflictException(4L, 5L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().details())
                .isEqualTo(Map.of("expectedVersion", 4L, "currentVersion", 5L));
    }
}
