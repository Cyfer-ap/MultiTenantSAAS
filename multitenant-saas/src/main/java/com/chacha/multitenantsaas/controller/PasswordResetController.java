package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.ResetPasswordRequest;
import com.chacha.multitenantsaas.dto.ResetPasswordResponse;
import com.chacha.multitenantsaas.dto.VerifiedPasswordResetRequest;
import com.chacha.multitenantsaas.service.PasswordResetService;
import com.chacha.multitenantsaas.service.VerifiedPasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
@Tag(name = "Password Reset", description = "Verified password recovery APIs")
public class PasswordResetController {

    private final VerifiedPasswordResetService verifiedPasswordResetService;
    private final PasswordResetService passwordResetService;

    public PasswordResetController(
            VerifiedPasswordResetService verifiedPasswordResetService,
            PasswordResetService passwordResetService) {
        this.verifiedPasswordResetService = verifiedPasswordResetService;
        this.passwordResetService = passwordResetService;
    }

    @Operation(
            summary = "Request password reset",
            description =
                    "Requires a one-time verified-email workspace grant. "
                            + "The reset token is delivered through transactional email.")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> requestPasswordReset(
            @Valid @RequestBody VerifiedPasswordResetRequest request) {
        ForgotPasswordResponse response =
                verifiedPasswordResetService.requestPasswordReset(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        VerifiedPasswordResetService.GENERIC_RESPONSE_MESSAGE, response));
    }

    @Operation(
            summary = "Complete password reset",
            description =
                    "Uses a single-use reset token to set a new password and revoke active sessions.")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = passwordResetService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", response));
    }
}
