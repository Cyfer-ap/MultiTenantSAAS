package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.EmailVerificationProperties;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartResponse;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyResponse;
import com.chacha.multitenantsaas.dto.WorkspaceLoginOptionResponse;
import com.chacha.multitenantsaas.email.EmailMessage;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.EmailVerificationChallenge;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.TrustedEmailBrowser;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.EmailVerificationChallengeRepository;
import com.chacha.multitenantsaas.repository.TrustedEmailBrowserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailWorkspaceDiscoveryService {

    private static final String VERIFICATION_ALGORITHM = "HmacSHA256";
    private static final long RESEND_COOLDOWN_SECONDS = 60L;
    private static final String GENERIC_START_MESSAGE =
            "If an active account exists for that email, a verification code has been sent.";

    private final AppUserRepository appUserRepository;
    private final EmailVerificationChallengeRepository challengeRepository;
    private final TrustedEmailBrowserRepository trustedEmailBrowserRepository;
    private final EmailSender emailSender;
    private final SecureTokenService secureTokenService;
    private final long expirationMinutes;
    private final int maxAttempts;
    private final long trustedBrowserDays;
    private final byte[] verificationSecret;
    private final boolean requireLoginGrant;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailWorkspaceDiscoveryService(
            AppUserRepository appUserRepository,
            EmailVerificationChallengeRepository challengeRepository,
            TrustedEmailBrowserRepository trustedEmailBrowserRepository,
            EmailSender emailSender,
            SecureTokenService secureTokenService,
            EmailVerificationProperties properties) {
        this.appUserRepository = appUserRepository;
        this.challengeRepository = challengeRepository;
        this.trustedEmailBrowserRepository = trustedEmailBrowserRepository;
        this.emailSender = emailSender;
        this.secureTokenService = secureTokenService;

        if (properties.getExpirationMinutes() <= 0L
                || properties.getMaxAttempts() <= 0
                || properties.getTrustedBrowserDays() <= 0L) {
            throw new IllegalStateException("Email verification settings must be positive");
        }

        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException("Email verification secret must not be blank");
        }

        this.expirationMinutes = properties.getExpirationMinutes();
        this.maxAttempts = properties.getMaxAttempts();
        this.trustedBrowserDays = properties.getTrustedBrowserDays();
        this.verificationSecret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.requireLoginGrant = properties.isRequireLoginGrant();
    }

    @Transactional
    public WorkspaceDiscoveryStartResponse start(WorkspaceDiscoveryStartRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Instant now = Instant.now();

        TrustedEmailBrowser trustedBrowser =
                findActiveTrustedBrowser(normalizedEmail, request.trustedBrowserToken(), now);

        if (trustedBrowser != null) {
            trustedBrowser.setLastUsedAt(now);
            trustedEmailBrowserRepository.save(trustedBrowser);

            List<WorkspaceLoginOptionResponse> workspaces = findWorkspaceOptions(normalizedEmail);
            EmailVerificationChallenge loginGrant = createVerifiedLoginGrant(normalizedEmail, now);

            return new WorkspaceDiscoveryStartResponse(
                    false,
                    null,
                    workspaces,
                    loginGrant.getId(),
                    0L,
                    "This browser is already trusted for email verification.");
        }

        List<WorkspaceLoginOptionResponse> workspaces = findWorkspaceOptions(normalizedEmail);
        UUID publicChallengeId = UUID.randomUUID();

        if (workspaces.isEmpty()) {
            return new WorkspaceDiscoveryStartResponse(
                    true,
                    publicChallengeId,
                    List.of(),
                    null,
                    expirationMinutes * 60L,
                    GENERIC_START_MESSAGE);
        }

        List<EmailVerificationChallenge> unusedChallenges =
                challengeRepository.findUnusedByEmailForUpdate(normalizedEmail);

        EmailVerificationChallenge recentChallenge =
                unusedChallenges.stream()
                        .filter(challenge -> !challenge.isExpired(now))
                        .max((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                        .orElse(null);

        if (recentChallenge != null
                && recentChallenge
                        .getCreatedAt()
                        .isAfter(now.minus(RESEND_COOLDOWN_SECONDS, ChronoUnit.SECONDS))) {
            return new WorkspaceDiscoveryStartResponse(
                    true,
                    recentChallenge.getId(),
                    List.of(),
                    null,
                    expirationMinutes * 60L,
                    GENERIC_START_MESSAGE);
        }

        unusedChallenges.forEach(challenge -> challenge.markUsed(now));
        challengeRepository.saveAll(unusedChallenges);

        String verificationCode = generateVerificationCode();
        String codeHash = hashVerificationCode(normalizedEmail, verificationCode);
        EmailVerificationChallenge challenge =
                new EmailVerificationChallenge(
                        normalizedEmail, codeHash, now.plus(expirationMinutes, ChronoUnit.MINUTES));

        challengeRepository.save(challenge);
        sendVerificationEmail(normalizedEmail, verificationCode);

        return new WorkspaceDiscoveryStartResponse(
                true,
                challenge.getId(),
                List.of(),
                null,
                expirationMinutes * 60L,
                GENERIC_START_MESSAGE);
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public WorkspaceDiscoveryVerifyResponse verify(WorkspaceDiscoveryVerifyRequest request) {
        EmailVerificationChallenge challenge =
                challengeRepository
                        .findByIdForUpdate(request.challengeId())
                        .orElseThrow(this::invalidCode);

        Instant now = Instant.now();

        if (challenge.isUsed()
                || challenge.isExpired(now)
                || challenge.getFailedAttempts() >= maxAttempts) {
            throw invalidCode();
        }

        String submittedHash = hashVerificationCode(challenge.getEmail(), request.code());

        if (!constantTimeEquals(challenge.getCodeHash(), submittedHash)) {
            challenge.recordFailedAttempt();

            if (challenge.getFailedAttempts() >= maxAttempts) {
                challenge.markUsed(now);
            }

            challengeRepository.save(challenge);
            throw invalidCode();
        }

        challenge.markUsed(now);
        challengeRepository.save(challenge);

        List<WorkspaceLoginOptionResponse> workspaces = findWorkspaceOptions(challenge.getEmail());

        String trustedBrowserToken = null;

        if (request.trustBrowser() && !workspaces.isEmpty()) {
            trustedBrowserToken = issueTrustedBrowserToken(challenge.getEmail(), now);
        }

        return new WorkspaceDiscoveryVerifyResponse(
                workspaces,
                workspaces.isEmpty() ? null : challenge.getId(),
                trustedBrowserToken,
                "Email verified successfully.");
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public EmailVerificationChallenge requireActiveLoginGrant(UUID workspaceGrantId, String email) {
        if (!requireLoginGrant) {
            return null;
        }

        if (workspaceGrantId == null) {
            throw invalidLoginGrant();
        }

        String normalizedEmail = normalizeEmail(email);
        EmailVerificationChallenge challenge =
                challengeRepository
                        .findByIdForUpdate(workspaceGrantId)
                        .orElseThrow(this::invalidLoginGrant);

        Instant now = Instant.now();

        if (!challenge.isUsed()
                || challenge.getUsedAt() == null
                || challenge.isLoginConsumed()
                || !challenge.getEmail().equals(normalizedEmail)
                || !now.isBefore(
                        challenge.getUsedAt().plus(expirationMinutes, ChronoUnit.MINUTES))) {
            throw invalidLoginGrant();
        }

        return challenge;
    }

    public void consumeLoginGrant(EmailVerificationChallenge challenge) {
        if (!requireLoginGrant) {
            return;
        }

        if (challenge == null || challenge.isLoginConsumed()) {
            throw invalidLoginGrant();
        }

        challenge.markLoginConsumed(Instant.now());
        challengeRepository.save(challenge);
    }

    private EmailVerificationChallenge createVerifiedLoginGrant(
            String normalizedEmail, Instant now) {
        String syntheticCodeHash =
                hashVerificationCode(normalizedEmail, generateVerificationCode());

        EmailVerificationChallenge challenge =
                new EmailVerificationChallenge(
                        normalizedEmail,
                        syntheticCodeHash,
                        now.plus(expirationMinutes, ChronoUnit.MINUTES));

        challenge.markUsed(now);
        return challengeRepository.save(challenge);
    }

    private TrustedEmailBrowser findActiveTrustedBrowser(
            String normalizedEmail, String rawToken, Instant now) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String tokenHash = secureTokenService.hashToken(rawToken.trim());

        return trustedEmailBrowserRepository
                .findByTokenHash(tokenHash)
                .filter(browser -> browser.getEmail().equals(normalizedEmail))
                .filter(browser -> browser.isActive(now))
                .orElse(null);
    }

    private String issueTrustedBrowserToken(String normalizedEmail, Instant now) {
        String rawToken = secureTokenService.generateToken();
        String tokenHash = secureTokenService.hashToken(rawToken);

        trustedEmailBrowserRepository.save(
                new TrustedEmailBrowser(
                        normalizedEmail, tokenHash, now.plus(trustedBrowserDays, ChronoUnit.DAYS)));

        return rawToken;
    }

    private List<WorkspaceLoginOptionResponse> findWorkspaceOptions(String normalizedEmail) {
        return appUserRepository.findByEmailWithTenant(normalizedEmail).stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> user.getTenant().getStatus() == TenantStatus.ACTIVE)
                .filter(this::hasUsablePassword)
                .map(
                        user ->
                                new WorkspaceLoginOptionResponse(
                                        user.getTenant().getId(),
                                        user.getTenant().getName(),
                                        user.getTenant().getSlug()))
                .toList();
    }

    private boolean hasUsablePassword(AppUser user) {
        return user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
    }

    private void sendVerificationEmail(String email, String code) {
        String html =
                """
                <div style="font-family:Arial,sans-serif;line-height:1.5;color:#1f2937">
                  <h2 style="margin-bottom:8px">Verify your email</h2>
                  <p>Use this code to continue signing in to MultiTenant SaaS:</p>
                  <div style="font-size:32px;font-weight:700;letter-spacing:8px;margin:24px 0">%s</div>
                  <p>This code expires in %d minutes.</p>
                  <p>If you did not request this code, you can ignore this email.</p>
                </div>
                """
                        .formatted(code, expirationMinutes);

        emailSender.send(new EmailMessage(email, "Your MultiTenant SaaS verification code", html));
    }

    private String generateVerificationCode() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    private String hashVerificationCode(String normalizedEmail, String code) {
        try {
            Mac mac = Mac.getInstance(VERIFICATION_ALGORITHM);
            mac.init(new SecretKeySpec(verificationSecret, VERIFICATION_ALGORITHM));

            byte[] digest =
                    mac.doFinal(
                            (normalizedEmail + ":" + code.trim()).getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash email verification code", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthenticationFailedException invalidCode() {
        return new AuthenticationFailedException("Verification code is invalid or expired");
    }

    private AuthenticationFailedException invalidLoginGrant() {
        return new AuthenticationFailedException(
                "Email verification is required before workspace login");
    }
}
