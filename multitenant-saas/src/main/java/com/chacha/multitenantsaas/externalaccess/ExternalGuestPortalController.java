package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/guest-portal")
public class ExternalGuestPortalController {

    public static final String SESSION_HEADER = "X-Guest-Session";

    private final ExternalGuestSessionService sessionService;
    private final ExternalPortalService portalService;

    public ExternalGuestPortalController(
            ExternalGuestSessionService sessionService, ExternalPortalService portalService) {
        this.sessionService = sessionService;
        this.portalService = portalService;
    }

    @PostMapping("/exchange")
    public ResponseEntity<ApiResponse<ExternalAccessDtos.ExchangeResponse>> exchange(
            @Valid @RequestBody ExternalAccessDtos.ExchangeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Guest invitation exchanged",
                        sessionService.exchange(request.invitationToken())));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<ExternalAccessDtos.SessionResponse>> session(
            @RequestHeader(SESSION_HEADER) String sessionToken) {
        return ResponseEntity.ok(
                ApiResponse.success("Guest session fetched", portalService.session(sessionToken)));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<ExternalAccessDtos.TasksResponse>> tasks(
            @RequestHeader(SESSION_HEADER) String sessionToken) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Shared project tasks fetched", portalService.tasks(sessionToken)));
    }
}
