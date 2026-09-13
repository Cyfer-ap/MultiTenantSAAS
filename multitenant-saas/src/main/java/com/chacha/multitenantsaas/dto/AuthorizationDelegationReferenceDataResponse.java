package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse.ScopeTargetOption;
import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse.UserOption;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthorizationDelegationReferenceDataResponse(
        List<UserOption> users,
        List<AuthorizationRoleResponse> roles,
        List<ParentAssignmentOption> parentAssignments,
        List<ScopeTargetOption> organizationalUnits,
        List<ScopeTargetOption> projects) {

    public record ParentAssignmentOption(
            UUID id,
            UUID roleId,
            String roleCode,
            String roleName,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId,
            Instant validFrom,
            Instant validUntil,
            List<String> permissionCodes) {}
}
