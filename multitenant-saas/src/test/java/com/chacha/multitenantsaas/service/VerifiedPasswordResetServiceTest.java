package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.VerifiedPasswordResetRequest;
import com.chacha.multitenantsaas.email.EmailMessage;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.entity.EmailVerificationChallenge;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifiedPasswordResetServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID GRANT_ID = UUID.randomUUID();
    private static final String EMAIL = "grace@example.com";

    @Mock private PasswordResetService passwordResetService;
    @Mock private EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService;
    @Mock private EmailSender emailSender;
    @Mock private EmailVerificationChallenge loginGrant;

    private VerifiedPasswordResetService service;

    @BeforeEach
    void setUp() {
        service =
                new VerifiedPasswordResetService(
                        passwordResetService,
                        emailWorkspaceDiscoveryService,
                        emailSender,
                        15L,
                        "https://app.example.test",
                        false);
    }

    @Test
    void verifiedRequestSendsSingleUseResetLinkAndConsumesGrant() {
        when(emailWorkspaceDiscoveryService.requireActiveLoginGrant(GRANT_ID, EMAIL))
                .thenReturn(loginGrant);
        when(passwordResetService.forgotPassword(
                        TENANT_ID, new com.chacha.multitenantsaas.dto.ForgotPasswordRequest(EMAIL)))
                .thenReturn(new ForgotPasswordResponse("generated", "raw-reset-token"));

        ForgotPasswordResponse response =
                service.requestPasswordReset(
                        new VerifiedPasswordResetRequest(TENANT_ID, EMAIL, GRANT_ID));

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        verify(emailWorkspaceDiscoveryService).consumeLoginGrant(loginGrant);

        assertThat(messageCaptor.getValue().to()).isEqualTo(EMAIL);
        assertThat(messageCaptor.getValue().subject()).contains("Reset");
        assertThat(messageCaptor.getValue().htmlContent())
                .contains("https://app.example.test/reset-password?token=raw-reset-token")
                .contains("15 minutes");

        assertThat(response.message())
                .isEqualTo(VerifiedPasswordResetService.GENERIC_RESPONSE_MESSAGE);
        assertThat(response.devResetToken()).isNull();
    }

    @Test
    void mailFailureDoesNotConsumeVerifiedGrant() {
        when(emailWorkspaceDiscoveryService.requireActiveLoginGrant(GRANT_ID, EMAIL))
                .thenReturn(loginGrant);
        when(passwordResetService.forgotPassword(
                        TENANT_ID, new com.chacha.multitenantsaas.dto.ForgotPasswordRequest(EMAIL)))
                .thenReturn(new ForgotPasswordResponse("generated", "raw-reset-token"));
        org.mockito.Mockito.doThrow(new IllegalStateException("mail unavailable"))
                .when(emailSender)
                .send(any(EmailMessage.class));

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.requestPasswordReset(
                                new VerifiedPasswordResetRequest(TENANT_ID, EMAIL, GRANT_ID)));

        verify(emailWorkspaceDiscoveryService, never()).consumeLoginGrant(loginGrant);
    }

    @Test
    void mismatchedWorkspaceReturnsGenericResponseAndBurnsGrant() {
        when(emailWorkspaceDiscoveryService.requireActiveLoginGrant(GRANT_ID, EMAIL))
                .thenReturn(loginGrant);
        when(passwordResetService.forgotPassword(
                        TENANT_ID, new com.chacha.multitenantsaas.dto.ForgotPasswordRequest(EMAIL)))
                .thenThrow(new ResourceNotFoundException("not found"));

        ForgotPasswordResponse response =
                service.requestPasswordReset(
                        new VerifiedPasswordResetRequest(TENANT_ID, EMAIL, GRANT_ID));

        verify(emailSender, never()).send(any());
        verify(emailWorkspaceDiscoveryService).consumeLoginGrant(loginGrant);
        assertThat(response.devResetToken()).isNull();
        assertThat(response.message())
                .isEqualTo(VerifiedPasswordResetService.GENERIC_RESPONSE_MESSAGE);
    }
}
