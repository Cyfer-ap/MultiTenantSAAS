package com.chacha.multitenantsaas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.AppUserRoleUpdateRequest;
import com.chacha.multitenantsaas.dto.AppUserStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.SystemAdminStatusUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AccountStateConcurrencyLockingTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SYSTEM_ADMIN_ID = UUID.randomUUID();
    private static final String EMAIL = "account-state@example.test";
    private static final String CURRENT_PASSWORD = "Password@123";
    private static final String NEW_PASSWORD = "NewPassword@123";

    @Mock private AppUserRepository appUserRepository;

    @Mock private TenantRepository tenantRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private AuditLogService auditLogService;

    @Mock private CurrentActorService currentActorService;

    @Mock private TenantAdminGuardService tenantAdminGuardService;

    @Mock private RefreshTokenService refreshTokenService;

    @Mock private CurrentSystemAdminService currentSystemAdminService;

    @Mock private LoginAttemptService loginAttemptService;

    @Mock private AuthorizationProvisioningService authorizationProvisioningService;

    @Mock private SubscriptionQuotaGuardService subscriptionQuotaGuardService;

    @Mock private SystemAdminRepository systemAdminRepository;

    @Mock private SystemAdminGuardService systemAdminGuardService;

    @Mock private PlatformAuditLogService platformAuditLogService;

    @Mock private Jwt jwt;

    @Mock private SystemAdmin actorSystemAdmin;

    private AppUserService appUserService;
    private SystemAuthService systemAuthService;
    private SystemAdminManagementService systemAdminManagementService;

    @BeforeEach
    void setUp() {
        appUserService =
                new AppUserService(
                        appUserRepository,
                        tenantRepository,
                        passwordEncoder,
                        auditLogService,
                        currentActorService,
                        tenantAdminGuardService,
                        refreshTokenService,
                        currentSystemAdminService,
                        loginAttemptService,
                        authorizationProvisioningService,
                        subscriptionQuotaGuardService);

        systemAuthService =
                new SystemAuthService(
                        systemAdminRepository, passwordEncoder, null, loginAttemptService);

        systemAdminManagementService =
                new SystemAdminManagementService(
                        systemAdminRepository,
                        passwordEncoder,
                        currentSystemAdminService,
                        systemAdminGuardService,
                        loginAttemptService,
                        platformAuditLogService);
    }

    @Test
    void roleChangeLocksTargetAndInvalidatesSessionVersion() {
        AppUser user = newTenantUser();

        when(appUserRepository.findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(user));
        when(currentSystemAdminService.isSystemAdminToken(jwt)).thenReturn(true);
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt))
                .thenReturn(actorSystemAdmin);
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, AppUser.class));

        appUserService.updateUserRole(
                TENANT_ID, USER_ID, new AppUserRoleUpdateRequest(UserRole.TENANT_ADMIN), jwt);

        assertEquals(1L, user.getSessionVersion());
        verify(appUserRepository).findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID);
        verify(appUserRepository, never()).findByTenantIdAndId(TENANT_ID, USER_ID);
        verify(refreshTokenService).revokeAllActiveTokensForUser(USER_ID);
    }

    @Test
    void statusChangeLocksTargetAndInvalidatesSessionVersion() {
        AppUser user = newTenantUser();

        when(appUserRepository.findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(user));
        when(currentSystemAdminService.isSystemAdminToken(jwt)).thenReturn(true);
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt))
                .thenReturn(actorSystemAdmin);
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, AppUser.class));

        appUserService.updateUserStatus(
                TENANT_ID, USER_ID, new AppUserStatusUpdateRequest(UserStatus.INACTIVE), jwt);

        assertEquals(1L, user.getSessionVersion());
        verify(appUserRepository).findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID);
        verify(refreshTokenService).revokeAllActiveTokensForUser(USER_ID);
    }

    @Test
    void deactivationLocksTargetAndInvalidatesSessionVersion() {
        AppUser user = newTenantUser();

        when(appUserRepository.findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(user));
        when(currentSystemAdminService.isSystemAdminToken(jwt)).thenReturn(true);
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt))
                .thenReturn(actorSystemAdmin);
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, AppUser.class));

        appUserService.deactivateUser(TENANT_ID, USER_ID, jwt);

        assertEquals(1L, user.getSessionVersion());
        verify(appUserRepository).findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID);
        verify(refreshTokenService).revokeAllActiveTokensForUser(USER_ID);
    }

    @Test
    void unlockUserLoginUsesWriteLockedTarget() {
        AppUser user = newTenantUser();

        when(appUserRepository.findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(user));
        when(currentSystemAdminService.isSystemAdminToken(jwt)).thenReturn(true);
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt))
                .thenReturn(actorSystemAdmin);

        appUserService.unlockUserLogin(TENANT_ID, USER_ID, jwt);

        verify(appUserRepository).findByTenantIdAndIdForUpdate(TENANT_ID, USER_ID);
        verify(appUserRepository, never()).findByTenantIdAndId(TENANT_ID, USER_ID);
        verify(loginAttemptService).unlockUser(user);
    }

    @Test
    void systemAdminPasswordChangeUsesWriteLockedTarget() {
        when(jwt.getClaimAsString("role")).thenReturn("SYSTEM_ADMIN");
        when(jwt.getClaimAsString("accountType")).thenReturn("SYSTEM_ADMIN");
        when(jwt.getSubject()).thenReturn(SYSTEM_ADMIN_ID.toString());
        when(systemAdminRepository.findByIdForUpdate(SYSTEM_ADMIN_ID)).thenReturn(Optional.empty());

        assertThrows(
                AuthenticationFailedException.class,
                () ->
                        systemAuthService.changePassword(
                                jwt,
                                new ChangePasswordRequest(
                                        CURRENT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)));

        verify(systemAdminRepository).findByIdForUpdate(SYSTEM_ADMIN_ID);
        verify(systemAdminRepository, never()).findById(SYSTEM_ADMIN_ID);
    }

    @Test
    void systemAdminStatusChangeUsesWriteLockedTarget() {
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt))
                .thenReturn(actorSystemAdmin);
        when(systemAdminRepository.findByIdForUpdate(SYSTEM_ADMIN_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        systemAdminManagementService.updateSystemAdminStatus(
                                SYSTEM_ADMIN_ID,
                                new SystemAdminStatusUpdateRequest(UserStatus.INACTIVE),
                                jwt));

        verify(systemAdminRepository).findByIdForUpdate(SYSTEM_ADMIN_ID);
    }

    @Test
    void systemAdminUnlockUsesWriteLockedTarget() {
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt))
                .thenReturn(actorSystemAdmin);
        when(systemAdminRepository.findByIdForUpdate(SYSTEM_ADMIN_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> systemAdminManagementService.unlockSystemAdminLogin(SYSTEM_ADMIN_ID, jwt));

        verify(systemAdminRepository).findByIdForUpdate(SYSTEM_ADMIN_ID);
    }

    private AppUser newTenantUser() {
        Tenant tenant = new Tenant("Account State Tenant", "account-state-tenant");
        tenant.setId(TENANT_ID);

        AppUser user =
                new AppUser(
                        tenant, "Account State User", EMAIL, "password-hash", UserRole.TENANT_USER);
        user.setId(USER_ID);

        return user;
    }
}
