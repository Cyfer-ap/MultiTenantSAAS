package com.chacha.multitenantsaas.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.LoginRequest;
import com.chacha.multitenantsaas.dto.SystemAdminLoginRequest;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.JwtContextService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AuthenticationLoginLockingTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String EMAIL = "login-lock@example.test";
    private static final String PASSWORD = "Password@123";

    @Mock private TenantRepository tenantRepository;

    @Mock private AppUserRepository appUserRepository;

    @Mock private SystemAdminRepository systemAdminRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtService jwtService;

    @Mock private JwtContextService jwtContextService;

    @Mock private RefreshTokenService refreshTokenService;

    @Mock private AuditLogService auditLogService;

    @Mock private LoginAttemptService loginAttemptService;

    private AuthService authService;
    private SystemAuthService systemAuthService;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(
                        tenantRepository,
                        appUserRepository,
                        passwordEncoder,
                        jwtService,
                        jwtContextService,
                        refreshTokenService,
                        auditLogService,
                        loginAttemptService);

        systemAuthService =
                new SystemAuthService(
                        systemAdminRepository, passwordEncoder, jwtService, loginAttemptService);
    }

    @Test
    void tenantLoginUsesWriteLockedUserLookup() {
        Tenant tenant = new Tenant("Login Lock Tenant", "login-lock-tenant");
        tenant.setId(TENANT_ID);

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(appUserRepository.findByTenantIdAndEmailForUpdate(TENANT_ID, EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login(TENANT_ID, new LoginRequest(EMAIL, PASSWORD)));

        verify(appUserRepository).findByTenantIdAndEmailForUpdate(TENANT_ID, EMAIL);
        verify(appUserRepository, never()).findByTenantIdAndEmail(TENANT_ID, EMAIL);
    }

    @Test
    void systemAdminLoginUsesWriteLockedLookup() {
        when(systemAdminRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                AuthenticationFailedException.class,
                () -> systemAuthService.login(new SystemAdminLoginRequest(EMAIL, PASSWORD)));

        verify(systemAdminRepository).findByEmailForUpdate(EMAIL);
        verify(systemAdminRepository, never()).findByEmail(EMAIL);
    }

    @Test
    void loginTransactionsCommitAuthenticationFailures() throws NoSuchMethodException {
        Method tenantLogin = AuthService.class.getMethod("login", UUID.class, LoginRequest.class);
        Method systemAdminLogin =
                SystemAuthService.class.getMethod("login", SystemAdminLoginRequest.class);

        Transactional tenantTransactional = tenantLogin.getAnnotation(Transactional.class);
        Transactional systemAdminTransactional =
                systemAdminLogin.getAnnotation(Transactional.class);

        assertNotNull(tenantTransactional);
        assertNotNull(systemAdminTransactional);

        assertTrue(
                Arrays.asList(tenantTransactional.noRollbackFor())
                        .contains(AuthenticationFailedException.class));
        assertTrue(
                Arrays.asList(systemAdminTransactional.noRollbackFor())
                        .contains(AuthenticationFailedException.class));
    }
}
