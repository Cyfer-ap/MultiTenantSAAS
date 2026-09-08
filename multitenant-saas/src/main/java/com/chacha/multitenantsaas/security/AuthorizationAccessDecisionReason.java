package com.chacha.multitenantsaas.security;

public enum AuthorizationAccessDecisionReason {
    GRANTED_BY_ROLE_ASSIGNMENT,
    INVALID_INPUT,
    INVALID_PERMISSION_CODE,
    TENANT_UNAVAILABLE,
    SUBJECT_UNAVAILABLE,
    NO_EFFECTIVE_GRANT,
    SCOPE_NOT_SATISFIED
}
