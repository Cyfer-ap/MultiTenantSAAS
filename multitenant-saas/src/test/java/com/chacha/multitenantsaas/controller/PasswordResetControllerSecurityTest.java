package com.chacha.multitenantsaas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.VerifiedPasswordResetRequest;
import com.chacha.multitenantsaas.service.PasswordResetService;
import com.chacha.multitenantsaas.service.VerifiedPasswordResetService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PasswordResetControllerSecurityTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID WORKSPACE_GRANT_ID = UUID.randomUUID();
    private static final VerifiedPasswordResetRequest REQUEST =
            new VerifiedPasswordResetRequest(TENANT_ID, "user@example.test", WORKSPACE_GRANT_ID);

    @Test
    void verifiedRequestReturnsGenericSuccessWithoutRawToken() {
        VerifiedPasswordResetService verifiedService = mock(VerifiedPasswordResetService.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        PasswordResetController controller =
                new PasswordResetController(verifiedService, passwordResetService);

        when(verifiedService.requestPasswordReset(REQUEST))
                .thenReturn(
                        new ForgotPasswordResponse(
                                VerifiedPasswordResetService.GENERIC_RESPONSE_MESSAGE, null));

        assertGenericResponse(controller.requestPasswordReset(REQUEST), null);
        verify(verifiedService).requestPasswordReset(REQUEST);
    }

    @Test
    void developmentTokenIsReturnedOnlyWhenVerifiedServiceProvidesIt() {
        VerifiedPasswordResetService verifiedService = mock(VerifiedPasswordResetService.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        PasswordResetController controller =
                new PasswordResetController(verifiedService, passwordResetService);

        when(verifiedService.requestPasswordReset(REQUEST))
                .thenReturn(
                        new ForgotPasswordResponse(
                                VerifiedPasswordResetService.GENERIC_RESPONSE_MESSAGE,
                                "raw-reset-token"));

        assertGenericResponse(controller.requestPasswordReset(REQUEST), "raw-reset-token");
        verify(verifiedService).requestPasswordReset(REQUEST);
    }

    private void assertGenericResponse(
            ResponseEntity<ApiResponse<ForgotPasswordResponse>> response, String expectedToken) {
        ApiResponse<ForgotPasswordResponse> body = response.getBody();

        assertNotNull(body);
        assertEquals(VerifiedPasswordResetService.GENERIC_RESPONSE_MESSAGE, body.message());
        assertNotNull(body.data());
        assertEquals(VerifiedPasswordResetService.GENERIC_RESPONSE_MESSAGE, body.data().message());

        if (expectedToken == null) {
            assertNull(body.data().devResetToken());
        } else {
            assertEquals(expectedToken, body.data().devResetToken());
        }
    }
}
