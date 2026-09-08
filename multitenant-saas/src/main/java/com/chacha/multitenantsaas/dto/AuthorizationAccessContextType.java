package com.chacha.multitenantsaas.dto;

public enum AuthorizationAccessContextType {
    TENANT,
    USER,
    PROJECT,
    ORGANIZATIONAL_UNIT,
    ORGANIZATIONAL_SUBTREE,
    DIRECT_REPORTS_ANCHOR
}
