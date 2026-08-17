package com.chacha.multitenantsaas.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.ForgotPasswordRequest;
import com.chacha.multitenantsaas.dto.ResetPasswordRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.PasswordResetToken;
import com.chacha.multitenantsaas.entity.RefreshToken;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.PasswordResetTokenRepository;
import com.chacha.multitenantsaas.repository.RefreshTokenRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthenticatedUserContext;
import com.chacha.multitenantsaas.security.JwtContextService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class SessionCredentialConcurrencyLockingTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "session-lock@example.test";
    private static final String CURRENT_PASSWORD = "Password@123";
    private static final String NEW_PASSWORD = "NewPassword@123";
    private static final String RAW_TOKEN = "raw-token";

    @Mock TenantRepository tenantRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock JwtContextService jwtContextService;
    @Mock AuditLogService auditLogService;
    @Mock LoginAttemptService loginAttemptService;
    @Mock EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService;
    @Mock Jwt jwt;

    private RefreshTokenService refreshTokenService;
    private AuthService authService;
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        refreshTokenService =
                new RefreshTokenService(refreshTokenRepository, appUserRepository, 30L);
        authService =
                new AuthService(
                        tenantRepository,
                        appUserRepository,
                        passwordEncoder,
                        jwtService,
                        jwtContextService,
                        refreshTokenService,
                        auditLogService,
                        loginAttemptService,
                        emailWorkspaceDiscoveryService);
        passwordResetService =
                new PasswordResetService(
                        tenantRepository,
                        appUserRepository,
                        passwordResetTokenRepository,
                        passwordEncoder,
                        refreshTokenService,
                        auditLogService,
                        30L);
    }

    private AuthenticatedUserContext currentUser() {
        return new AuthenticatedUserContext(
                TENANT_ID, USER_ID, EMAIL, "Session User", UserRole.TENANT_USER);
    }

    @Test
    void logoutAllUsesWriteLockedUserLookup() {
        when(jwtContextService.getCurrentUser(jwt)).thenReturn(currentUser());
        when(tenantRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(new Tenant("Tenant", "tenant")));
        when(appUserRepository.findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class, () -> authService.logoutAllDevices(jwt));

        verify(appUserRepository).findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID);
    }

    @Test
    void changePasswordUsesWriteLockedUserLookup() {
        when(jwtContextService.getCurrentUser(jwt)).thenReturn(currentUser());
        when(appUserRepository.findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                AuthenticationFailedException.class,
                () ->
                        authService.changePassword(
                                jwt,
                                new ChangePasswordRequest(
                                        CURRENT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)));

        verify(appUserRepository).findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID);
    }

    @Test
    void forgotPasswordUsesWriteLockedEmailLookup() {
        Tenant tenant = new Tenant("Tenant", "tenant");
        tenant.setId(TENANT_ID);

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(appUserRepository.findByTenantIdAndEmailForUpdate(TENANT_ID, EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        passwordResetService.forgotPassword(
                                TENANT_ID, new ForgotPasswordRequest(EMAIL)));

        verify(appUserRepository).findByTenantIdAndEmailForUpdate(TENANT_ID, EMAIL);
        verify(appUserRepository, never()).findByTenantIdAndEmail(TENANT_ID, EMAIL);
    }

    @Test
    void resetPasswordUsesUserThenTokenLockOrder() {
        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        PasswordResetToken lockedToken = org.mockito.Mockito.mock(PasswordResetToken.class);

        when(passwordResetTokenRepository.findUserIdByTokenHash(anyString()))
                .thenReturn(Optional.of(USER_ID));
        when(appUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(lockedToken));
        when(lockedToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(lockedToken.isActive()).thenReturn(false);

        assertThrows(
                AuthenticationFailedException.class,
                () ->
                        passwordResetService.resetPassword(
                                new ResetPasswordRequest(RAW_TOKEN, NEW_PASSWORD, NEW_PASSWORD)));

        InOrder order = inOrder(passwordResetTokenRepository, appUserRepository);
        order.verify(passwordResetTokenRepository).findUserIdByTokenHash(anyString());
        order.verify(appUserRepository).findByIdForUpdate(USER_ID);
        order.verify(passwordResetTokenRepository).findByTokenHashForUpdate(anyString());
    }

    @Test
    void refreshRotationUsesUserThenTokenLockOrder() {
        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        RefreshToken lockedToken = org.mockito.Mockito.mock(RefreshToken.class);

        when(refreshTokenRepository.findUserIdByTokenHash(anyString()))
                .thenReturn(Optional.of(USER_ID));
        when(appUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(lockedToken));
        when(lockedToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(lockedToken.isActive()).thenReturn(false);

        assertThrows(
                AuthenticationFailedException.class,
                () -> refreshTokenService.rotateRefreshToken(RAW_TOKEN));

        InOrder order = inOrder(refreshTokenRepository, appUserRepository);
        order.verify(refreshTokenRepository).findUserIdByTokenHash(anyString());
        order.verify(appUserRepository).findByIdForUpdate(USER_ID);
        order.verify(refreshTokenRepository).findByTokenHashForUpdate(anyString());
    }
}
