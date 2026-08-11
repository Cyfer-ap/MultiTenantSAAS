package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.ForgotPasswordRequest;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.ResetPasswordRequest;
import com.chacha.multitenantsaas.dto.ResetPasswordResponse;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@Tag(
        name = "Password Reset",
        description = "Forgot password and reset password APIs"
)
public class PasswordResetController {

    private static final String FORGOT_PASSWORD_RESPONSE_MESSAGE =
            "If an active account matches those details, password reset "
                    + "instructions will be sent.";

    private final PasswordResetService passwordResetService;
    private final boolean exposeResetToken;

    public PasswordResetController(
            PasswordResetService passwordResetService,
            @Value("${app.password-reset.expose-token:false}")
            boolean exposeResetToken
    ) {
        this.passwordResetService = passwordResetService;
        this.exposeResetToken = exposeResetToken;
    }

    @Operation(
            summary = "Request password reset",
            description = "Returns the same response whether or not the account "
                    + "exists. A raw token is included only when the explicit "
                    + "development exposure setting is enabled."
    )
    @PostMapping("/api/tenants/{tenantId}/auth/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        ForgotPasswordResponse internalResponse;

        try {
            internalResponse = passwordResetService
                    .forgotPassword(tenantId, request);
        } catch (
                ResourceNotFoundException
                        | AuthenticationFailedException exception
        ) {
            internalResponse = new ForgotPasswordResponse(
                    FORGOT_PASSWORD_RESPONSE_MESSAGE,
                    null
            );
        }

        String devResetToken = exposeResetToken
                ? internalResponse.devResetToken()
                : null;

        ForgotPasswordResponse response =
                new ForgotPasswordResponse(
                        FORGOT_PASSWORD_RESPONSE_MESSAGE,
                        devResetToken
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        FORGOT_PASSWORD_RESPONSE_MESSAGE,
                        response
                )
        );
    }

    @Operation(
            summary = "Reset password",
            description = "Uses a valid reset token to set a new password and revoke active refresh tokens."
    )
    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        ResetPasswordResponse response = passwordResetService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password reset successfully", response)
        );
    }
}