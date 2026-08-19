package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.dto.NotificationMarkAllReadResponse;
import com.chacha.multitenantsaas.dto.NotificationPreferenceResponse;
import com.chacha.multitenantsaas.dto.NotificationPreferenceUpdateRequest;
import com.chacha.multitenantsaas.dto.NotificationResponse;
import com.chacha.multitenantsaas.dto.NotificationUnreadCountResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.NotificationPreferenceService;
import com.chacha.multitenantsaas.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final CurrentActorService currentActorService;

    public NotificationController(
            NotificationService notificationService,
            NotificationPreferenceService notificationPreferenceService,
            CurrentActorService currentActorService) {
        this.notificationService = notificationService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.currentActorService = currentActorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser recipient = currentActorService.getRequiredActiveActor(tenantId, jwt);
        PageResponse<NotificationResponse> response =
                notificationService.getNotifications(
                        tenantId,
                        recipient.getId(),
                        PageRequest.of(
                                PaginationUtils.validatePage(page),
                                PaginationUtils.validateSize(size)));

        return noStore(ApiResponse.success("Notifications fetched successfully", response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        AppUser recipient = currentActorService.getRequiredActiveActor(tenantId, jwt);
        NotificationUnreadCountResponse response =
                new NotificationUnreadCountResponse(
                        notificationService.countUnread(tenantId, recipient.getId()));

        return noStore(
                ApiResponse.success("Notification unread count fetched successfully", response));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceResponse>>> getPreferences(
            @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        AppUser recipient = currentActorService.getRequiredActiveActor(tenantId, jwt);
        List<NotificationPreferenceResponse> response =
                notificationPreferenceService.getPreferences(tenantId, recipient.getId());

        return noStore(
                ApiResponse.success("Notification preferences fetched successfully", response));
    }

    @PatchMapping("/preferences/{type}")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreference(
            @PathVariable UUID tenantId,
            @PathVariable NotificationType type,
            @Valid @RequestBody NotificationPreferenceUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser recipient = currentActorService.getRequiredActiveActor(tenantId, jwt);
        NotificationPreferenceResponse response =
                notificationPreferenceService.updateEmailPreference(
                        recipient.getTenant(), recipient, type, request.emailEnabled());

        return noStore(
                ApiResponse.success("Notification preference updated successfully", response));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable UUID tenantId,
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser recipient = currentActorService.getRequiredActiveActor(tenantId, jwt);
        NotificationResponse response =
                notificationService.markRead(tenantId, recipient.getId(), notificationId);

        return noStore(ApiResponse.success("Notification marked as read", response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<NotificationMarkAllReadResponse>> markAllRead(
            @PathVariable UUID tenantId, @AuthenticationPrincipal Jwt jwt) {
        AppUser recipient = currentActorService.getRequiredActiveActor(tenantId, jwt);
        NotificationMarkAllReadResponse response =
                new NotificationMarkAllReadResponse(
                        notificationService.markAllRead(tenantId, recipient.getId()));

        return noStore(ApiResponse.success("Notifications marked as read", response));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
