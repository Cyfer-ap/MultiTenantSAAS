package com.chacha.multitenantsaas.exception;

import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;

public class SubscriptionRestrictionException extends RuntimeException {

    public enum RestrictionType {
        SERVICE_UNAVAILABLE,
        USER_LIMIT_REACHED,
        PROJECT_LIMIT_REACHED,
        WORKSPACE_READ_ONLY
    }

    private final RestrictionType restrictionType;
    private final SubscriptionAccessReason accessReason;
    private final String resource;
    private final Long used;
    private final Long limit;

    public SubscriptionRestrictionException(
            RestrictionType restrictionType,
            SubscriptionAccessReason accessReason,
            String resource,
            Long used,
            Long limit,
            String message) {
        super(message);
        this.restrictionType = restrictionType;
        this.accessReason = accessReason;
        this.resource = resource;
        this.used = used;
        this.limit = limit;
    }

    public RestrictionType getRestrictionType() {
        return restrictionType;
    }

    public SubscriptionAccessReason getAccessReason() {
        return accessReason;
    }

    public String getResource() {
        return resource;
    }

    public Long getUsed() {
        return used;
    }

    public Long getLimit() {
        return limit;
    }
}
