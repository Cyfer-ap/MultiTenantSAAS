package com.chacha.multitenantsaas.entity;

public enum AuditAction {
    TENANT_ONBOARDED,
    USER_CREATED,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    LOGOUT_ALL,
    TOKEN_REFRESH,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED
}