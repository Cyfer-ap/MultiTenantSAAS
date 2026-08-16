package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.observability.AuthenticationMetrics;
import com.chacha.multitenantsaas.observability.AuthenticationMetrics.AccountType;
import com.chacha.multitenantsaas.observability.AuthenticationMetrics.LoginOutcome;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final AppUserRepository appUserRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final int maxFailedAttempts;
    private final long lockMinutes;
    private final AuthenticationMetrics authenticationMetrics;

    public LoginAttemptService(
            AppUserRepository appUserRepository,
            SystemAdminRepository systemAdminRepository,
            @Value("${app.security.max-failed-login-attempts:5}") int maxFailedAttempts,
            @Value("${app.security.account-lock-minutes:15}") long lockMinutes,
            AuthenticationMetrics authenticationMetrics) {
        this.appUserRepository = appUserRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
        this.authenticationMetrics = authenticationMetrics;
    }

    public void ensureNotLocked(AppUser user) {
        if (user.getLockedUntil() == null) {
            return;
        }

        if (user.getLockedUntil().isAfter(Instant.now())) {
            authenticationMetrics.recordLoginAttempt(AccountType.TENANT_USER, LoginOutcome.BLOCKED);
            throw new AuthenticationFailedException(
                    "Account is temporarily locked. Please try again later.");
        }

        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        appUserRepository.save(user);
    }

    public void ensureNotLocked(SystemAdmin systemAdmin) {
        if (systemAdmin.getLockedUntil() == null) {
            return;
        }

        if (systemAdmin.getLockedUntil().isAfter(Instant.now())) {
            authenticationMetrics.recordLoginAttempt(
                    AccountType.SYSTEM_ADMIN, LoginOutcome.BLOCKED);
            throw new AuthenticationFailedException(
                    "System admin account is temporarily locked. Please try again later.");
        }

        systemAdmin.setLockedUntil(null);
        systemAdmin.setFailedLoginAttempts(0);
        systemAdminRepository.save(systemAdmin);
    }

    public void recordFailedAttempt(AppUser user) {
        int failedAttempts = user.getFailedLoginAttempts() + 1;
        boolean accountLocked = failedAttempts >= maxFailedAttempts;

        user.setFailedLoginAttempts(failedAttempts);

        if (accountLocked) {
            user.setLockedUntil(Instant.now().plus(lockMinutes, ChronoUnit.MINUTES));
        }

        appUserRepository.save(user);
        authenticationMetrics.recordLoginAttempt(AccountType.TENANT_USER, LoginOutcome.FAILURE);

        if (accountLocked) {
            authenticationMetrics.recordAccountLock(AccountType.TENANT_USER);
        }
    }

    public void recordFailedAttempt(SystemAdmin systemAdmin) {
        int failedAttempts = systemAdmin.getFailedLoginAttempts() + 1;
        boolean accountLocked = failedAttempts >= maxFailedAttempts;

        systemAdmin.setFailedLoginAttempts(failedAttempts);

        if (accountLocked) {
            systemAdmin.setLockedUntil(Instant.now().plus(lockMinutes, ChronoUnit.MINUTES));
        }

        systemAdminRepository.save(systemAdmin);
        authenticationMetrics.recordLoginAttempt(AccountType.SYSTEM_ADMIN, LoginOutcome.FAILURE);

        if (accountLocked) {
            authenticationMetrics.recordAccountLock(AccountType.SYSTEM_ADMIN);
        }
    }

    public void recordSuccessfulLogin(AppUser user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        appUserRepository.save(user);
        authenticationMetrics.recordLoginAttempt(AccountType.TENANT_USER, LoginOutcome.SUCCESS);
    }

    public void recordSuccessfulLogin(SystemAdmin systemAdmin) {
        systemAdmin.setFailedLoginAttempts(0);
        systemAdmin.setLockedUntil(null);

        systemAdminRepository.save(systemAdmin);
        authenticationMetrics.recordLoginAttempt(AccountType.SYSTEM_ADMIN, LoginOutcome.SUCCESS);
    }

    public void unlockUser(AppUser user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        appUserRepository.save(user);
    }

    public void unlockSystemAdmin(SystemAdmin systemAdmin) {
        systemAdmin.setFailedLoginAttempts(0);
        systemAdmin.setLockedUntil(null);

        systemAdminRepository.save(systemAdmin);
    }
}
