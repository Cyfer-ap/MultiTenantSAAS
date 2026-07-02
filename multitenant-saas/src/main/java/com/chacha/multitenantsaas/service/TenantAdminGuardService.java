package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantAdminGuardService {

    private final AppUserRepository appUserRepository;

    public TenantAdminGuardService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public void ensureCanChangeRole(
            AppUser actorUser,
            AppUser targetUser,
            UserRole newRole
    ) {
        if (isSameUser(actorUser, targetUser)) {
            throw new IllegalArgumentException("You cannot change your own role");
        }

        ensureTenantKeepsActiveAdminAfterRoleChange(targetUser, newRole);
    }

    public void ensureCanChangeRole(
            AppUser targetUser,
            UserRole newRole
    ) {
        ensureTenantKeepsActiveAdminAfterRoleChange(targetUser, newRole);
    }

    public void ensureCanChangeStatus(
            AppUser actorUser,
            AppUser targetUser,
            UserStatus newStatus
    ) {
        if (isSameUser(actorUser, targetUser) && newStatus != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("You cannot deactivate or suspend your own account");
        }

        ensureTenantKeepsActiveAdminAfterStatusChange(targetUser, newStatus);
    }

    public void ensureCanChangeStatus(
            AppUser targetUser,
            UserStatus newStatus
    ) {
        ensureTenantKeepsActiveAdminAfterStatusChange(targetUser, newStatus);
    }

    public void ensureCanDeactivate(AppUser actorUser, AppUser targetUser) {
        if (isSameUser(actorUser, targetUser)) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }

        ensureTenantKeepsActiveAdminAfterDeactivation(targetUser);
    }

    public void ensureCanDeactivate(AppUser targetUser) {
        ensureTenantKeepsActiveAdminAfterDeactivation(targetUser);
    }

    private void ensureTenantKeepsActiveAdminAfterRoleChange(
            AppUser targetUser,
            UserRole newRole
    ) {
        if (isRemovingActiveAdminAccess(targetUser, newRole)) {
            ensureAnotherActiveAdminExists(targetUser);
        }
    }

    private void ensureTenantKeepsActiveAdminAfterStatusChange(
            AppUser targetUser,
            UserStatus newStatus
    ) {
        if (isDisablingActiveAdmin(targetUser, newStatus)) {
            ensureAnotherActiveAdminExists(targetUser);
        }
    }

    private void ensureTenantKeepsActiveAdminAfterDeactivation(AppUser targetUser) {
        if (targetUser.getRole() == UserRole.TENANT_ADMIN
                && targetUser.getStatus() == UserStatus.ACTIVE) {
            ensureAnotherActiveAdminExists(targetUser);
        }
    }

    private boolean isSameUser(AppUser actorUser, AppUser targetUser) {
        return actorUser.getId().equals(targetUser.getId());
    }

    private boolean isRemovingActiveAdminAccess(AppUser targetUser, UserRole newRole) {
        return targetUser.getRole() == UserRole.TENANT_ADMIN
                && targetUser.getStatus() == UserStatus.ACTIVE
                && newRole != UserRole.TENANT_ADMIN;
    }

    private boolean isDisablingActiveAdmin(AppUser targetUser, UserStatus newStatus) {
        return targetUser.getRole() == UserRole.TENANT_ADMIN
                && targetUser.getStatus() == UserStatus.ACTIVE
                && newStatus != UserStatus.ACTIVE;
    }

    private void ensureAnotherActiveAdminExists(AppUser targetUser) {
        long activeAdminCount = appUserRepository.countByTenantIdAndRoleAndStatus(
                targetUser.getTenant().getId(),
                UserRole.TENANT_ADMIN,
                UserStatus.ACTIVE
        );

        if (activeAdminCount <= 1) {
            throw new IllegalArgumentException("At least one active tenant admin must remain");
        }
    }
}