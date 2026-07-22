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

    public UserInvitationService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            UserInvitationRepository userInvitationRepository,
            PasswordEncoder passwordEncoder,
            SecureTokenService secureTokenService,
            CurrentActorService currentActorService,
            CurrentSystemAdminService currentSystemAdminService,
            AuditLogService auditLogService,
            @Value("${app.user-invitation.expiration-hours:48}") long expirationHours
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.userInvitationRepository = userInvitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureTokenService = secureTokenService;
        this.currentActorService = currentActorService;
        this.currentSystemAdminService = currentSystemAdminService;
        this.auditLogService = auditLogService;
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

        AppUser user = new AppUser(
                tenant,
                invitation.getFullName(),
                invitation.getEmail(),
                passwordEncoder.encode(request.newPassword()),
                invitation.getRole()
        );

        AppUser savedUser = appUserRepository.save(user);

        invitation.setStatus(UserInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        userInvitationRepository.save(invitation);

        recordUserCreationAudit(invitation, savedUser);

        return new UserInvitationAcceptResponse(
                mapToUserResponse(savedUser),
                "Invitation accepted successfully. You can now log in."
        );
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