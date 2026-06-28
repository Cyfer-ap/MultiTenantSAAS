package com.chacha.multitenantsaas.dto;

public record TenantOnboardingResponse(
        TenantResponse tenant,
        AppUserResponse adminUser,
        String message
) {
}
