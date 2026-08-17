package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.config.EmailVerificationProperties;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartResponse;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyResponse;
import com.chacha.multitenantsaas.email.EmailMessage;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.EmailVerificationChallenge;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TrustedEmailBrowser;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.EmailVerificationChallengeRepository;
import com.chacha.multitenantsaas.repository.TrustedEmailBrowserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailWorkspaceDiscoveryServiceTest {

    private static final String EMAIL = "grace@example.com";
    private static final Pattern CODE_PATTERN = Pattern.compile(">(\\d{6})</div>");

    @Mock private AppUserRepository appUserRepository;
    @Mock private EmailVerificationChallengeRepository challengeRepository;
    @Mock private TrustedEmailBrowserRepository trustedEmailBrowserRepository;
    @Mock private EmailSender emailSender;

    private SecureTokenService secureTokenService;
    private EmailWorkspaceDiscoveryService service;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setExpirationMinutes(10L);
        properties.setMaxAttempts(5);
        properties.setTrustedBrowserDays(30L);
        properties.setSecret("email-verification-test-secret-with-enough-entropy");

        secureTokenService = new SecureTokenService();
        service =
                new EmailWorkspaceDiscoveryService(
                        appUserRepository,
                        challengeRepository,
                        trustedEmailBrowserRepository,
                        emailSender,
                        secureTokenService,
                        properties);
    }

    @Test
    void sendsCodeThenReturnsWorkspaceAfterSuccessfulVerification() {
        AppUser user = activeUser();
        when(appUserRepository.findByEmailWithTenant(EMAIL)).thenReturn(List.of(user));
        when(challengeRepository.save(any(EmailVerificationChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(trustedEmailBrowserRepository.save(any(TrustedEmailBrowser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceDiscoveryStartResponse start =
                service.start(new WorkspaceDiscoveryStartRequest(EMAIL, null));

        ArgumentCaptor<EmailVerificationChallenge> challengeCaptor =
                ArgumentCaptor.forClass(EmailVerificationChallenge.class);
        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);

        verify(challengeRepository).save(challengeCaptor.capture());
        verify(emailSender).send(emailCaptor.capture());

        String verificationCode = extractCode(emailCaptor.getValue().htmlContent());
        EmailVerificationChallenge challenge = challengeCaptor.getValue();

        assertThat(start.verificationRequired()).isTrue();
        assertThat(start.challengeId()).isEqualTo(challenge.getId());
        assertThat(start.workspaces()).isEmpty();

        when(challengeRepository.findByIdForUpdate(challenge.getId()))
                .thenReturn(Optional.of(challenge));

        WorkspaceDiscoveryVerifyResponse verified =
                service.verify(
                        new WorkspaceDiscoveryVerifyRequest(
                                challenge.getId(), verificationCode, true));

        assertThat(verified.workspaces()).hasSize(1);
        assertThat(verified.workspaces().getFirst().tenantId()).isEqualTo(user.getTenant().getId());
        assertThat(verified.workspaces().getFirst().name()).isEqualTo("Research Lab");
        assertThat(verified.workspaceGrantId()).isEqualTo(challenge.getId());
        assertThat(verified.trustedBrowserToken()).isNotBlank();
        assertThat(challenge.isUsed()).isTrue();

        EmailVerificationChallenge loginGrant =
                service.requireActiveLoginGrant(verified.workspaceGrantId(), EMAIL);

        service.consumeLoginGrant(loginGrant);

        assertThat(challenge.isLoginConsumed()).isTrue();
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.requireActiveLoginGrant(verified.workspaceGrantId(), EMAIL));
    }

    @Test
    void loginGrantIsRequiredByDefault() {
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.requireActiveLoginGrant(null, EMAIL));
    }

    @Test
    void wrongCodeConsumesChallengeAfterConfiguredAttemptLimit() {
        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setExpirationMinutes(10L);
        properties.setMaxAttempts(1);
        properties.setTrustedBrowserDays(30L);
        properties.setSecret("email-verification-test-secret-with-enough-entropy");

        service =
                new EmailWorkspaceDiscoveryService(
                        appUserRepository,
                        challengeRepository,
                        trustedEmailBrowserRepository,
                        emailSender,
                        secureTokenService,
                        properties);

        when(appUserRepository.findByEmailWithTenant(EMAIL)).thenReturn(List.of(activeUser()));
        when(challengeRepository.save(any(EmailVerificationChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceDiscoveryStartResponse start =
                service.start(new WorkspaceDiscoveryStartRequest(EMAIL, null));

        ArgumentCaptor<EmailVerificationChallenge> challengeCaptor =
                ArgumentCaptor.forClass(EmailVerificationChallenge.class);
        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(challengeRepository).save(challengeCaptor.capture());
        verify(emailSender).send(emailCaptor.capture());

        EmailVerificationChallenge challenge = challengeCaptor.getValue();
        String issuedCode = extractCode(emailCaptor.getValue().htmlContent());
        String wrongCode = issuedCode.equals("000000") ? "000001" : "000000";

        when(challengeRepository.findByIdForUpdate(start.challengeId()))
                .thenReturn(Optional.of(challenge));

        assertThrows(
                AuthenticationFailedException.class,
                () ->
                        service.verify(
                                new WorkspaceDiscoveryVerifyRequest(
                                        start.challengeId(), wrongCode, false)));

        assertThat(challenge.getFailedAttempts()).isEqualTo(1);
        assertThat(challenge.isUsed()).isTrue();
    }

    @Test
    void trustedBrowserSkipsVerificationEmail() {
        AppUser user = activeUser();
        String rawToken = secureTokenService.generateToken();
        TrustedEmailBrowser browser =
                new TrustedEmailBrowser(
                        EMAIL,
                        secureTokenService.hashToken(rawToken),
                        Instant.now().plus(30, ChronoUnit.DAYS));

        when(appUserRepository.findByEmailWithTenant(EMAIL)).thenReturn(List.of(user));
        when(trustedEmailBrowserRepository.findByTokenHash(secureTokenService.hashToken(rawToken)))
                .thenReturn(Optional.of(browser));
        when(trustedEmailBrowserRepository.save(browser)).thenReturn(browser);
        when(challengeRepository.save(any(EmailVerificationChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceDiscoveryStartResponse response =
                service.start(new WorkspaceDiscoveryStartRequest(EMAIL, rawToken));

        assertThat(response.verificationRequired()).isFalse();
        assertThat(response.challengeId()).isNull();
        assertThat(response.workspaces()).hasSize(1);
        assertThat(response.workspaceGrantId()).isNotNull();
        verify(emailSender, never()).send(any());
        verify(challengeRepository).save(any(EmailVerificationChallenge.class));
    }

    private AppUser activeUser() {
        Tenant tenant = new Tenant("Research Lab", "research-lab");
        tenant.setId(UUID.randomUUID());

        AppUser user =
                new AppUser(
                        tenant,
                        "Grace Hopper",
                        EMAIL,
                        "$2a$10$test-password-hash",
                        UserRole.TENANT_ADMIN);
        user.setId(UUID.randomUUID());
        return user;
    }

    private String extractCode(String html) {
        Matcher matcher = CODE_PATTERN.matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
