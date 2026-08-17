package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.ForgotPasswordRequest;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.VerifiedPasswordResetRequest;
import com.chacha.multitenantsaas.email.EmailMessage;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.entity.EmailVerificationChallenge;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifiedPasswordResetService {

    public static final String GENERIC_RESPONSE_MESSAGE =
            "If an active account matches the verified workspace, password reset instructions "
                    + "will be sent.";

    private final PasswordResetService passwordResetService;
    private final EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService;
    private final EmailSender emailSender;
    private final long expirationMinutes;
    private final String frontendBaseUrl;
    private final boolean exposeResetToken;

    public VerifiedPasswordResetService(
            PasswordResetService passwordResetService,
            EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService,
            EmailSender emailSender,
            @Value("${app.password-reset.expiration-minutes}") long expirationMinutes,
            @Value("${app.password-reset.frontend-base-url:http://localhost:8080}")
                    String frontendBaseUrl,
            @Value("${app.password-reset.expose-token:false}") boolean exposeResetToken) {
        this.passwordResetService = passwordResetService;
        this.emailWorkspaceDiscoveryService = emailWorkspaceDiscoveryService;
        this.emailSender = emailSender;
        this.expirationMinutes = expirationMinutes;
        this.frontendBaseUrl = validateAndNormalizeFrontendBaseUrl(frontendBaseUrl);
        this.exposeResetToken = exposeResetToken;

        if (expirationMinutes <= 0L) {
            throw new IllegalStateException("Password-reset expiration must be positive");
        }
    }

    @Transactional
    public ForgotPasswordResponse requestPasswordReset(VerifiedPasswordResetRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        EmailVerificationChallenge loginGrant =
                emailWorkspaceDiscoveryService.requireActiveLoginGrant(
                        request.workspaceGrantId(), normalizedEmail);

        ForgotPasswordResponse generatedResponse;

        try {
            generatedResponse =
                    passwordResetService.forgotPassword(
                            request.tenantId(), new ForgotPasswordRequest(normalizedEmail));
        } catch (ResourceNotFoundException | AuthenticationFailedException exception) {
            // Consume a valid verification grant even when the selected workspace/account
            // does not match. This prevents one verified grant being reused to probe tenant IDs.
            emailWorkspaceDiscoveryService.consumeLoginGrant(loginGrant);
            return new ForgotPasswordResponse(GENERIC_RESPONSE_MESSAGE, null);
        }

        String rawResetToken = generatedResponse.devResetToken();

        if (rawResetToken == null || rawResetToken.isBlank()) {
            throw new IllegalStateException("Password reset service did not issue a reset token");
        }

        sendResetEmail(normalizedEmail, rawResetToken);

        // Only consume the grant after delivery succeeds. If the mail provider fails,
        // the transaction rolls back and the verified user can retry.
        emailWorkspaceDiscoveryService.consumeLoginGrant(loginGrant);

        return new ForgotPasswordResponse(
                GENERIC_RESPONSE_MESSAGE, exposeResetToken ? rawResetToken : null);
    }

    private void sendResetEmail(String email, String rawResetToken) {
        String resetUrl =
                frontendBaseUrl
                        + "/reset-password?token="
                        + URLEncoder.encode(rawResetToken, StandardCharsets.UTF_8);

        String html =
                """
                <div style="font-family:Arial,sans-serif;line-height:1.5;color:#1f2937">
                  <h2 style="margin-bottom:8px">Reset your password</h2>
                  <p>Use the link below to choose a new MultiTenant SaaS password.</p>
                  <p style="margin:24px 0">
                    <a href="%s"
                       style="display:inline-block;padding:12px 18px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px">
                      Reset password
                    </a>
                  </p>
                  <p>This link expires in %d minutes and can be used only once.</p>
                  <p>If you did not request a password reset, you can ignore this email.</p>
                </div>
                """
                        .formatted(resetUrl, expirationMinutes);

        emailSender.send(new EmailMessage(email, "Reset your MultiTenant SaaS password", html));
    }

    private String validateAndNormalizeFrontendBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            throw new IllegalStateException("Password-reset frontend base URL must not be blank");
        }

        String normalized = rawBaseUrl.trim().replaceAll("/+$", "");
        URI uri;

        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Password-reset frontend base URL must be a valid HTTP(S) URL", exception);
        }

        String scheme = uri.getScheme();

        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException(
                    "Password-reset frontend base URL must be an HTTP(S) origin/base URL");
        }

        return normalized;
    }
}
