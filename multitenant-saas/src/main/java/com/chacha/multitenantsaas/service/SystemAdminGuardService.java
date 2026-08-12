package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemAdminGuardService {

    private final SystemAdminRepository systemAdminRepository;

    public SystemAdminGuardService(SystemAdminRepository systemAdminRepository) {
        this.systemAdminRepository = systemAdminRepository;
    }

    public void ensureCanChangeStatus(
            SystemAdmin actorSystemAdmin, SystemAdmin targetSystemAdmin, UserStatus newStatus) {
        if (isSameSystemAdmin(actorSystemAdmin, targetSystemAdmin)
                && newStatus != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "You cannot deactivate or suspend your own system admin account");
        }

        if (isDisablingActiveSystemAdmin(targetSystemAdmin, newStatus)) {
            ensureAnotherActiveSystemAdminExists();
        }
    }

    private boolean isSameSystemAdmin(SystemAdmin actorSystemAdmin, SystemAdmin targetSystemAdmin) {
        return actorSystemAdmin.getId().equals(targetSystemAdmin.getId());
    }

    private boolean isDisablingActiveSystemAdmin(
            SystemAdmin targetSystemAdmin, UserStatus newStatus) {
        return targetSystemAdmin.getStatus() == UserStatus.ACTIVE && newStatus != UserStatus.ACTIVE;
    }

    private void ensureAnotherActiveSystemAdminExists() {
        long activeSystemAdminCount = systemAdminRepository.countByStatus(UserStatus.ACTIVE);

        if (activeSystemAdminCount <= 1) {
            throw new IllegalArgumentException("At least one active system admin must remain");
        }
    }
}
