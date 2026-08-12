package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.UserInvitationAcceptRequest;
import com.chacha.multitenantsaas.dto.UserInvitationAcceptResponse;
import com.chacha.multitenantsaas.dto.UserInvitationCreateRequest;
import com.chacha.multitenantsaas.dto.UserInvitationDetailsResponse;
import com.chacha.multitenantsaas.dto.UserInvitationResponse;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.service.UserInvitationService;
import com.chacha.multitenantsaas.web.SubscriptionReadOnlyAllowed;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserInvitationController {

    private final UserInvitationService userInvitationService;

    public UserInvitationController(UserInvitationService userInvitationService) {
        this.userInvitationService = userInvitationService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'user.create'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @PostMapping("/tenants/{tenantId}/user-invitations")
    public ResponseEntity<ApiResponse<UserInvitationResponse>> createInvitation(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UserInvitationCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserInvitationResponse response =
                userInvitationService.createInvitation(tenantId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("User invitation created successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'user.read'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @GetMapping("/tenants/{tenantId}/user-invitations")
    public ResponseEntity<ApiResponse<PageResponse<UserInvitationDetailsResponse>>>
            getInvitationsByTenant(
                    @PathVariable UUID tenantId,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "10") int size,
                    @RequestParam(defaultValue = "createdAt") String sortBy,
                    @RequestParam(defaultValue = "desc") String sortDir,
                    @RequestParam(required = false) UserInvitationStatus status,
                    @RequestParam(required = false) UserRole role,
                    @RequestParam(required = false) String search) {
        Pageable pageable =
                PageRequest.of(
                        PaginationUtils.validatePage(page),
                        PaginationUtils.validateSize(size),
                        SortingUtils.getDirection(sortDir),
                        SortingUtils.validateSortBy(
                                sortBy,
                                "createdAt",
                                "createdAt",
                                "fullName",
                                "email",
                                "role",
                                "status",
                                "expiresAt"));

        PageResponse<UserInvitationDetailsResponse> response =
                userInvitationService.getInvitationsByTenant(
                        tenantId, status, role, search, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("User invitations fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'user.read'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @GetMapping("/tenants/{tenantId}/user-invitations/{invitationId}")
    public ResponseEntity<ApiResponse<UserInvitationDetailsResponse>> getInvitationById(
            @PathVariable UUID tenantId, @PathVariable UUID invitationId) {
        UserInvitationDetailsResponse response =
                userInvitationService.getInvitationById(tenantId, invitationId);

        return ResponseEntity.ok(
                ApiResponse.success("User invitation fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'user.create'"
                    + ")"
                    + " or "
                    + "@systemSecurity.isSystemAdmin()")
    @SubscriptionReadOnlyAllowed
    @PatchMapping("/tenants/{tenantId}/user-invitations/{invitationId}/revoke")
    public ResponseEntity<ApiResponse<UserInvitationDetailsResponse>> revokeInvitation(
            @PathVariable UUID tenantId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt) {
        UserInvitationDetailsResponse response =
                userInvitationService.revokeInvitation(tenantId, invitationId, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("User invitation revoked successfully", response));
    }

    @PostMapping("/user-invitations/accept")
    public ResponseEntity<ApiResponse<UserInvitationAcceptResponse>> acceptInvitation(
            @Valid @RequestBody UserInvitationAcceptRequest request) {
        UserInvitationAcceptResponse response = userInvitationService.acceptInvitation(request);

        return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully", response));
    }
}
