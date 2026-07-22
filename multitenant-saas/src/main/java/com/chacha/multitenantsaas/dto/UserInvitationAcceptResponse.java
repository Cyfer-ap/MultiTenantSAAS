package com.chacha.multitenantsaas.dto;

public record UserInvitationAcceptResponse(
        AppUserResponse user,
        String message
) {
}