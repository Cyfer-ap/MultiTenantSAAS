package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.ForgotPasswordRequest;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordResetControllerSecurityTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final ForgotPasswordRequest REQUEST =
            new ForgotPasswordRequest("user@example.test");
    private static final String GENERIC_MESSAGE =
            "If an active account matches those details, password reset "
                    + "instructions will be sent.";

    @Test
    void missingAccountReturnsGenericSuccess() {
        PasswordResetService service = mock(PasswordResetService.class);
        PasswordResetController controller =
                new PasswordResetController(service, false);

        when(service.forgotPassword(TENANT_ID, REQUEST))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertGenericResponse(
                controller.forgotPassword(TENANT_ID, REQUEST),
                null
        );
    }

    @Test
    void inactiveAccountReturnsSameGenericSuccess() {
        PasswordResetService service = mock(PasswordResetService.class);
        PasswordResetController controller =
                new PasswordResetController(service, false);

        when(service.forgotPassword(TENANT_ID, REQUEST))
                .thenThrow(
                        new AuthenticationFailedException(
                                "User account is not active"
                        )
                );

        assertGenericResponse(
                controller.forgotPassword(TENANT_ID, REQUEST),
                null
        );
    }

    @Test
    void productionResponseDoesNotExposeRawResetToken() {
        PasswordResetService service = mock(PasswordResetService.class);
        PasswordResetController controller =
                new PasswordResetController(service, false);

        when(service.forgotPassword(TENANT_ID, REQUEST))
                .thenReturn(
                        new ForgotPasswordResponse(
                                "internal message",
                                "raw-reset-token"
                        )
                );

        assertGenericResponse(
                controller.forgotPassword(TENANT_ID, REQUEST),
                null
        );
    }

    @Test
    void explicitDevelopmentSettingCanExposeRawResetToken() {
        PasswordResetService service = mock(PasswordResetService.class);
        PasswordResetController controller =
                new PasswordResetController(service, true);

        when(service.forgotPassword(TENANT_ID, REQUEST))
                .thenReturn(
                        new ForgotPasswordResponse(
                                "internal message",
                                "raw-reset-token"
                        )
                );

        assertGenericResponse(
                controller.forgotPassword(TENANT_ID, REQUEST),
                "raw-reset-token"
        );
    }

    private void assertGenericResponse(
            ResponseEntity<ApiResponse<ForgotPasswordResponse>> response,
            String expectedToken
    ) {
        ApiResponse<ForgotPasswordResponse> body = response.getBody();

        assertNotNull(body);
        assertEquals(GENERIC_MESSAGE, body.message());
        assertNotNull(body.data());
        assertEquals(GENERIC_MESSAGE, body.data().message());

        if (expectedToken == null) {
            assertNull(body.data().devResetToken());
        } else {
            assertEquals(
                    expectedToken,
                    body.data().devResetToken()
            );
        }
    }
}
