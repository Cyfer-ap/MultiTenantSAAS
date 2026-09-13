package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse.ScopeTargetOption;
import com.chacha.multitenantsaas.dto.AuthorizationAssignmentReferenceDataResponse.UserOption;
import java.util.List;

public record AuthorizationDelegationReferenceDataResponse(
        List<UserOption> users,
        List<AuthorizationRoleResponse> roles,
        List<AuthorizationUserRoleAssignmentResponse> parentAssignments,
        List<ScopeTargetOption> organizationalUnits,
        List<ScopeTargetOption> projects) {}
