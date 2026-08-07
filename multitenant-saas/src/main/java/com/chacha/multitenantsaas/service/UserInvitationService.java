package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.entity.*;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserInvitationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class UserInvitationService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService secureTokenService;
    private final CurrentActorService currentActorService;
    private final CurrentSystemAdminService currentSystemAdminService;
    private final AuditLogService auditLogService;
    private final long expirationHours;
    private final AuthorizationProvisioningService
            authorizationProvisioningService;
    private final SubscriptionQuotaGuardService
            subscriptionQuotaGuardService;

    public UserInvitationService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            UserInvitationRepository userInvitationRepository,
            PasswordEncoder passwordEncoder,
            SecureTokenService secureTokenService,
            CurrentActorService currentActorService,
            CurrentSystemAdminService currentSystemAdminService,
            AuditLogService auditLogService,
            AuthorizationProvisioningService
                    authorizationProvisioningService,
            SubscriptionQuotaGuardService
                    subscriptionQuotaGuardService,
            @Value(
                    "${app.user-invitation.expiration-hours:48}"
            )
            long expirationHours
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.userInvitationRepository =
                userInvitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureTokenService = secureTokenService;
        this.currentActorService = currentActorService;
        this.currentSystemAdminService =
                currentSystemAdminService;
        this.auditLogService = auditLogService;
        this.authorizationProvisioningService =
                authorizationProvisioningService;
        this.subscriptionQuotaGuardService =
                subscriptionQuotaGuardService;
        this.expirationHours = expirationHours;
    }

    @Transactional
    public UserInvitationResponse createInvitation(
            UUID tenantId,
            UserInvitationCreateRequest request,
            Jwt jwt
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AuthenticationFailedException(
                        "Tenant not found"
                ));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException(
                    "Tenant is not active"
            );
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        if (appUserRepository.existsByTenantIdAndEmail(
                tenantId,
                normalizedEmail
        )) {
            throw new DuplicateResourceException(
                    "User already exists for this tenant: " + normalizedEmail
            );
        }

        AppUser invitedByUser = null;
        SystemAdmin invitedBySystemAdmin = null;

        if (currentSystemAdminService.isSystemAdminToken(jwt)) {
            invitedBySystemAdmin =
                    currentSystemAdminService.getRequiredActiveSystemAdmin(jwt);
        } else {
            invitedByUser =
                    currentActorService.getRequiredActiveActor(tenantId, jwt);
        }

        revokeExistingPendingInvitations(tenantId, normalizedEmail);

        String rawToken = secureTokenService.generateToken();
        String tokenHash = secureTokenService.hashToken(rawToken);

        UserInvitation invitation = new UserInvitation(
                tenant,
                invitedByUser,
                invitedBySystemAdmin,
                request.fullName().trim(),
                normalizedEmail,
                request.role(),
                tokenHash,
                Instant.now().plus(expirationHours, ChronoUnit.HOURS)
        );

        UserInvitation savedInvitation =
                userInvitationRepository.save(invitation);

        return new UserInvitationResponse(
                savedInvitation.getId(),
                tenant.getId(),
                savedInvitation.getFullName(),
                savedInvitation.getEmail(),
                savedInvitation.getRole(),
                savedInvitation.getStatus(),
                savedInvitation.getExpiresAt(),
                rawToken,
                "Invitation generated successfully. In production, send this token by email."
        );
    }

    @Transactional
    public UserInvitationAcceptResponse acceptInvitation(
            UserInvitationAcceptRequest request
    ) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException(
                    "New password and confirm password do not match"
            );
        }

        String tokenHash =
                secureTokenService.hashToken(request.invitationToken());

        UserInvitation invitation =
                userInvitationRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new AuthenticationFailedException(
                                "Invalid invitation token"
                        ));

        if (!invitation.isActive()) {
            throw new AuthenticationFailedException(
                    "Invitation is expired, revoked, or already accepted"
            );
        }

        Tenant tenant = invitation.getTenant();

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException(
                    "Tenant is not active"
            );
        }

        if (appUserRepository.existsByTenantIdAndEmail(
                tenant.getId(),
                invitation.getEmail()
        )) {
            throw new DuplicateResourceException(
                    "User already exists for this tenant"
            );
        }

        subscriptionQuotaGuardService.requireUserSlot(
                tenant.getId()
        );

        AppUser user = new AppUser(
                tenant,
                invitation.getFullName(),
                invitation.getEmail(),
                passwordEncoder.encode(request.newPassword()),
                invitation.getRole()
        );

        AppUser savedUser =
                appUserRepository.saveAndFlush(user);

        authorizationProvisioningService
                .synchronizeUserFromLegacyState(
                        tenant.getId(),
                        savedUser.getId()
                );

        invitation.setStatus(UserInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        userInvitationRepository.save(invitation);

        recordUserCreationAudit(invitation, savedUser);

        return new UserInvitationAcceptResponse(
                mapToUserResponse(savedUser),
                "Invitation accepted successfully. You can now log in."
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<UserInvitationDetailsResponse> getInvitationsByTenant(
            UUID tenantId,
            UserInvitationStatus status,
            UserRole role,
            String search,
            Pageable pageable
    ) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException(
                    "Tenant not found with id: " + tenantId
            );
        }

        Page<UserInvitation> invitations =
                userInvitationRepository.findTenantInvitations(
                        tenantId,
                        status,
                        role,
                        normalizeSearch(search),
                        pageable
                );

        return new PageResponse<>(
                invitations.getContent()
                        .stream()
                        .map(this::mapToDetailsResponse)
                        .toList(),
                invitations.getNumber(),
                invitations.getSize(),
                invitations.getTotalElements(),
                invitations.getTotalPages(),
                invitations.isFirst(),
                invitations.isLast()
        );
    }

    @Transactional(readOnly = true)
    public UserInvitationDetailsResponse getInvitationById(
            UUID tenantId,
            UUID invitationId
    ) {
        UserInvitation invitation =
                getInvitationOrThrow(tenantId, invitationId);

        return mapToDetailsResponse(invitation);
    }

    @Transactional
    public UserInvitationDetailsResponse revokeInvitation(
            UUID tenantId,
            UUID invitationId,
            Jwt jwt
    ) {
        validateManagementActor(tenantId, jwt);

        UserInvitation invitation =
                getInvitationOrThrow(tenantId, invitationId);

        if (invitation.getStatus() == UserInvitationStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    "Accepted invitation cannot be revoked"
            );
        }

        if (invitation.getStatus() == UserInvitationStatus.REVOKED) {
            throw new IllegalArgumentException(
                    "Invitation is already revoked"
            );
        }

        invitation.setStatus(UserInvitationStatus.REVOKED);
        invitation.setRevokedAt(Instant.now());

        UserInvitation updatedInvitation =
                userInvitationRepository.save(invitation);

        return mapToDetailsResponse(updatedInvitation);
    }

    private void revokeExistingPendingInvitations(
            UUID tenantId,
            String email
    ) {
        List<UserInvitation> pendingInvitations =
                userInvitationRepository.findByTenant_IdAndEmailAndStatus(
                        tenantId,
                        email,
                        UserInvitationStatus.PENDING
                );

        Instant now = Instant.now();

        pendingInvitations.forEach(invitation -> {
            invitation.setStatus(UserInvitationStatus.REVOKED);
            invitation.setRevokedAt(now);
        });

        userInvitationRepository.saveAll(pendingInvitations);
    }

    private void recordUserCreationAudit(
            UserInvitation invitation,
            AppUser savedUser
    ) {
        if (invitation.getInvitedBySystemAdmin() != null) {
            auditLogService.recordSystemAdminSuccess(
                    invitation.getTenant(),
                    invitation.getInvitedBySystemAdmin(),
                    savedUser,
                    AuditAction.USER_CREATED,
                    "User created after accepting invitation: "
                            + savedUser.getEmail()
            );

            return;
        }

        if (invitation.getInvitedByUser() != null) {
            auditLogService.recordSuccess(
                    invitation.getTenant(),
                    invitation.getInvitedByUser(),
                    savedUser,
                    AuditAction.USER_CREATED,
                    "User created after accepting invitation: "
                            + savedUser.getEmail()
            );

            return;
        }

        auditLogService.recordSelfSuccess(
                invitation.getTenant(),
                savedUser,
                AuditAction.USER_CREATED,
                "User created after accepting invitation"
        );
    }

    private UserInvitation getInvitationOrThrow(
            UUID tenantId,
            UUID invitationId
    ) {
        return userInvitationRepository.findByTenant_IdAndId(
                        tenantId,
                        invitationId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invitation not found with id: "
                                + invitationId
                                + " for tenant: "
                                + tenantId
                ));
    }

    private void validateManagementActor(UUID tenantId, Jwt jwt) {
        if (currentSystemAdminService.isSystemAdminToken(jwt)) {
            currentSystemAdminService.getRequiredActiveSystemAdmin(jwt);
            return;
        }

        currentActorService.getRequiredActiveActor(tenantId, jwt);
    }

    private UserInvitationDetailsResponse mapToDetailsResponse(
            UserInvitation invitation
    ) {
        AppUser invitedByUser = invitation.getInvitedByUser();
        SystemAdmin invitedBySystemAdmin =
                invitation.getInvitedBySystemAdmin();

        boolean expired =
                invitation.getStatus() == UserInvitationStatus.PENDING
                        && invitation.isExpired();

        return new UserInvitationDetailsResponse(
                invitation.getId(),
                invitation.getTenant().getId(),
                invitation.getFullName(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.isActive(),
                expired,
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                invitation.getAcceptedAt(),
                invitation.getRevokedAt(),
                invitedByUser != null ? invitedByUser.getId() : null,
                invitedByUser != null ? invitedByUser.getEmail() : null,
                invitedBySystemAdmin != null
                        ? invitedBySystemAdmin.getId()
                        : null,
                invitedBySystemAdmin != null
                        ? invitedBySystemAdmin.getEmail()
                        : null
        );
    }

    private String normalizeSearch(String search) {
        if (search == null || search.trim().isBlank()) {
            return null;
        }

        return search.trim();
    }

    private AppUserResponse mapToUserResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getTenant().getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}