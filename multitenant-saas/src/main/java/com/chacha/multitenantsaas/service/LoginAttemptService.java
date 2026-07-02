package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class LoginAttemptService {

    private final AppUserRepository appUserRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final int maxFailedAttempts;
    private final long lockMinutes;

    public LoginAttemptService(
            AppUserRepository appUserRepository,
            SystemAdminRepository systemAdminRepository,
            @Value("${app.security.max-failed-login-attempts:5}") int maxFailedAttempts,
            @Value("${app.security.account-lock-minutes:15}") long lockMinutes
    ) {
        this.appUserRepository = appUserRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
    }

    public void ensureNotLocked(AppUser user) {
        if (user.getLockedUntil() == null) {
            return;
        }

        if (user.getLockedUntil().isAfter(Instant.now())) {
            throw new AuthenticationFailedException(
                    "Account is temporarily locked. Please try again later."
            );
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
            throw new AuthenticationFailedException(
                    "System admin account is temporarily locked. Please try again later."
            );
        }

        systemAdmin.setLockedUntil(null);
        systemAdmin.setFailedLoginAttempts(0);
        systemAdminRepository.save(systemAdmin);
    }

    public void recordFailedAttempt(AppUser user) {
        int failedAttempts = user.getFailedLoginAttempts() + 1;

        user.setFailedLoginAttempts(failedAttempts);

        if (failedAttempts >= maxFailedAttempts) {
            user.setLockedUntil(
                    Instant.now().plus(lockMinutes, ChronoUnit.MINUTES)
            );
        }

        appUserRepository.save(user);
    }

    public void recordFailedAttempt(SystemAdmin systemAdmin) {
        int failedAttempts = systemAdmin.getFailedLoginAttempts() + 1;

        systemAdmin.setFailedLoginAttempts(failedAttempts);

        if (failedAttempts >= maxFailedAttempts) {
            systemAdmin.setLockedUntil(
                    Instant.now().plus(lockMinutes, ChronoUnit.MINUTES)
            );
        }

        systemAdminRepository.save(systemAdmin);
    }

    public void recordSuccessfulLogin(AppUser user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        appUserRepository.save(user);
    }

    public void recordSuccessfulLogin(SystemAdmin systemAdmin) {
        systemAdmin.setFailedLoginAttempts(0);
        systemAdmin.setLockedUntil(null);

        systemAdminRepository.save(systemAdmin);
    }
}