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

import java.util.UUID;

@RestController
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

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