package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.ForgotPasswordRequest;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.ResetPasswordRequest;
import com.chacha.multitenantsaas.dto.ResetPasswordResponse;
import com.chacha.multitenantsaas.service.PasswordResetService;
import jakarta.validation.Valid;
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

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(
            summary = "Request password reset",
            description = "Generates a password reset token for an active tenant user. In development, the token is returned in the response."
    )
    @PostMapping("/api/tenants/{tenantId}/auth/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        ForgotPasswordResponse response = passwordResetService.forgotPassword(tenantId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Password reset token generated successfully", response)
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