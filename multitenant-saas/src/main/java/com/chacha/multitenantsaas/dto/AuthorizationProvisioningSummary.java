package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record AuthorizationProvisioningSummary(

        UUID tenantId,

        int systemRolesAvailable,

        int usersScanned,

        int assignmentsCreated,

        int assignmentsAlreadyPresent,

        int inactiveUsersSkipped
) {
}