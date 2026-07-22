package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.*;
import com.chacha.multitenantsaas.service.UserInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserInvitationController {

    private final UserInvitationService userInvitationService;

    public UserInvitationController(
            UserInvitationService userInvitationService
    ) {
        this.userInvitationService = userInvitationService;
    }

    @PreAuthorize(
            "@tenantSecurity.isTenantAdmin(#tenantId) " +
                    "or @systemSecurity.isSystemAdmin()"
    )
    @PostMapping("/tenants/{tenantId}/user-invitations")
    public ResponseEntity<ApiResponse<UserInvitationResponse>> createInvitation(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UserInvitationCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserInvitationResponse response =
                userInvitationService.createInvitation(
                        tenantId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User invitation created successfully",
                        response
                )
        );
    }

    @PostMapping("/user-invitations/accept")
    public ResponseEntity<ApiResponse<UserInvitationAcceptResponse>> acceptInvitation(
            @Valid @RequestBody UserInvitationAcceptRequest request
    ) {
        UserInvitationAcceptResponse response =
                userInvitationService.acceptInvitation(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Invitation accepted successfully",
                        response
                )
        );
    }
}