package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.observability.AuthenticationMetrics;
import com.chacha.multitenantsaas.observability.AuthenticationMetrics.AccountType;
import com.chacha.multitenantsaas.observability.AuthenticationMetrics.LoginOutcome;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceMetricsTest {

    @Mock private AppUserRepository appUserRepository;

    @Mock private SystemAdminRepository systemAdminRepository;

    @Mock private AuthenticationMetrics authenticationMetrics;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService =
                new LoginAttemptService(
                        appUserRepository, systemAdminRepository, 2, 15L, authenticationMetrics);
    }

    @Test
    void failedAttemptThatReachesThresholdRecordsFailureAndLock() {
        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getFailedLoginAttempts()).thenReturn(1);

        loginAttemptService.recordFailedAttempt(user);

        verify(user).setFailedLoginAttempts(2);
        verify(user).setLockedUntil(any(Instant.class));
        verify(appUserRepository).save(user);
        verify(authenticationMetrics)
                .recordLoginAttempt(AccountType.TENANT_USER, LoginOutcome.FAILURE);
        verify(authenticationMetrics).recordAccountLock(AccountType.TENANT_USER);
    }

    @Test
    void successfulSystemAdminLoginRecordsSuccess() {
        SystemAdmin systemAdmin = org.mockito.Mockito.mock(SystemAdmin.class);

        loginAttemptService.recordSuccessfulLogin(systemAdmin);

        verify(systemAdminRepository).save(systemAdmin);
        verify(authenticationMetrics)
                .recordLoginAttempt(AccountType.SYSTEM_ADMIN, LoginOutcome.SUCCESS);
    }

    @Test
    void lockedTenantUserRecordsBlockedAttempt() {
        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getLockedUntil()).thenReturn(Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> loginAttemptService.ensureNotLocked(user))
                .isInstanceOf(AuthenticationFailedException.class);

        verify(authenticationMetrics)
                .recordLoginAttempt(AccountType.TENANT_USER, LoginOutcome.BLOCKED);
    }
}
