package com.chacha.multitenantsaas.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.UserInvitationAcceptRequest;
import com.chacha.multitenantsaas.dto.UserInvitationCreateRequest;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserInvitation;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserInvitationRepository;
import java.util.List;
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
class UserInvitationServiceLockingTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID INVITATION_ID = UUID.randomUUID();
    private static final String EMAIL = "invitee@example.test";
    private static final String RAW_TOKEN = "raw-invitation-token";
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final String PASSWORD = "InviteePassword@123";

    @Mock private TenantRepository tenantRepository;

    @Mock private AppUserRepository appUserRepository;

    @Mock private UserInvitationRepository userInvitationRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private SecureTokenService secureTokenService;

    @Mock private CurrentActorService currentActorService;

    @Mock private CurrentSystemAdminService currentSystemAdminService;

    @Mock private AuditLogService auditLogService;

    @Mock private AuthorizationProvisioningService authorizationProvisioningService;

    @Mock private SubscriptionQuotaGuardService subscriptionQuotaGuardService;

    @Mock private Jwt jwt;

    @Mock private SystemAdmin systemAdmin;

    private UserInvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationService =
                new UserInvitationService(
                        tenantRepository,
                        appUserRepository,
                        userInvitationRepository,
                        passwordEncoder,
                        secureTokenService,
                        currentActorService,
                        currentSystemAdminService,
                        auditLogService,
                        authorizationProvisioningService,
                        subscriptionQuotaGuardService,
                        48L);
    }

    @Test
    void createInvitationLocksTenantBeforeReplacementLookup() {
        Tenant tenant = new Tenant("Invitation Lock Tenant", "invitation-lock-tenant");
        tenant.setId(TENANT_ID);

        when(tenantRepository.findByIdForUpdate(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(appUserRepository.existsByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(false);
        when(currentSystemAdminService.isSystemAdminToken(jwt)).thenReturn(true);
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt)).thenReturn(systemAdmin);
        when(userInvitationRepository.findByTenantIdAndEmailAndStatusForUpdate(
                        TENANT_ID, EMAIL, UserInvitationStatus.PENDING))
                .thenReturn(List.of());
        when(secureTokenService.generateToken()).thenReturn(RAW_TOKEN);
        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(userInvitationRepository.save(any(UserInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, UserInvitation.class));

        invitationService.createInvitation(
                TENANT_ID,
                new UserInvitationCreateRequest("Invitee", EMAIL, UserRole.TENANT_USER),
                jwt);

        InOrder order = inOrder(tenantRepository, appUserRepository, userInvitationRepository);

        order.verify(tenantRepository).findByIdForUpdate(TENANT_ID);
        order.verify(appUserRepository).existsByTenantIdAndEmail(TENANT_ID, EMAIL);
        order.verify(userInvitationRepository)
                .findByTenantIdAndEmailAndStatusForUpdate(
                        TENANT_ID, EMAIL, UserInvitationStatus.PENDING);

        verify(tenantRepository, never()).findById(TENANT_ID);
    }

    @Test
    void acceptInvitationUsesWriteLockedTokenLookup() {
        UserInvitation invitation = org.mockito.Mockito.mock(UserInvitation.class);

        when(secureTokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(userInvitationRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.of(invitation));
        when(invitation.isActive()).thenReturn(false);

        assertThrows(
                AuthenticationFailedException.class,
                () ->
                        invitationService.acceptInvitation(
                                new UserInvitationAcceptRequest(RAW_TOKEN, PASSWORD, PASSWORD)));

        verify(userInvitationRepository).findByTokenHashForUpdate(TOKEN_HASH);
        verify(userInvitationRepository, never()).findByTokenHash(TOKEN_HASH);
    }

    @Test
    void revokeInvitationUsesWriteLockedInvitationLookup() {
        UserInvitation invitation = org.mockito.Mockito.mock(UserInvitation.class);

        when(currentSystemAdminService.isSystemAdminToken(jwt)).thenReturn(true);
        when(currentSystemAdminService.getRequiredActiveSystemAdmin(jwt)).thenReturn(systemAdmin);
        when(userInvitationRepository.findByTenantIdAndIdForUpdate(TENANT_ID, INVITATION_ID))
                .thenReturn(Optional.of(invitation));
        when(invitation.getStatus()).thenReturn(UserInvitationStatus.ACCEPTED);

        assertThrows(
                IllegalArgumentException.class,
                () -> invitationService.revokeInvitation(TENANT_ID, INVITATION_ID, jwt));

        verify(userInvitationRepository).findByTenantIdAndIdForUpdate(TENANT_ID, INVITATION_ID);
        verify(userInvitationRepository, never()).findByTenant_IdAndId(TENANT_ID, INVITATION_ID);
    }
}
