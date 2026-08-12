package com.chacha.multitenantsaas.dto;

public record AuthorizationProvisioningBackfillResponse(
        AuthorizationProvisioningSummary provisioning,
        AuthorizationProvisioningReadinessResponse readiness) {}
