package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.config.OidcLoginProperties;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.dto.OidcAuthorizationStartRequest;
import com.chacha.multitenantsaas.dto.OidcAuthorizationStartResponse;
import com.chacha.multitenantsaas.dto.OidcSessionExchangeRequest;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.service.BrowserSessionCookieService;
import com.chacha.multitenantsaas.service.OidcAuthorizationService;
import com.chacha.multitenantsaas.service.OidcCallbackService;
import com.chacha.multitenantsaas.service.OidcSessionHandoffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@Tag(name = "OIDC Authentication", description = "Tenant-bound enterprise OIDC authentication")
public class OidcAuthenticationController {

    private final OidcAuthorizationService authorizationService;
    private final OidcCallbackService callbackService;
    private final OidcSessionHandoffService sessionHandoffService;
    private final BrowserSessionCookieService browserSessionCookieService;
    private final OidcLoginProperties oidcLoginProperties;

    public OidcAuthenticationController(
            OidcAuthorizationService authorizationService,
            OidcCallbackService callbackService,
            OidcSessionHandoffService sessionHandoffService,
            BrowserSessionCookieService browserSessionCookieService,
            OidcLoginProperties oidcLoginProperties) {
        this.authorizationService = authorizationService;
        this.callbackService = callbackService;
        this.sessionHandoffService = sessionHandoffService;
        this.browserSessionCookieService = browserSessionCookieService;
        this.oidcLoginProperties = oidcLoginProperties;
    }

    @PostMapping("/api/tenants/{tenantId}/auth/oidc/start")
    @Operation(summary = "Start tenant OIDC login")
    public ResponseEntity<ApiResponse<OidcAuthorizationStartResponse>> start(
            @PathVariable UUID tenantId,
            @RequestBody(required = false) OidcAuthorizationStartRequest request) {
        boolean keepSignedIn = request != null && Boolean.TRUE.equals(request.keepSignedIn());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "OIDC authorization started",
                                authorizationService.start(tenantId, keepSignedIn)));
    }

    @GetMapping("/api/auth/oidc/callback")
    @Operation(summary = "Complete provider callback and return the browser to the application")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false, name = "error") String providerError) {
        try {
            OidcSessionHandoffService.IssuedHandoff handoff =
                    callbackService.authenticate(state, code, providerError);
            return redirect(frontendCompletionUri("code", handoff.code()));
        } catch (AuthenticationFailedException exception) {
            return redirect(frontendCompletionUri("error", "authentication_failed"));
        }
    }

    @PostMapping("/api/auth/oidc/session")
    @Operation(summary = "Exchange a one-time OIDC browser handoff for an application session")
    public ResponseEntity<ApiResponse<LoginResponse>> exchangeSession(
            @Valid @RequestBody OidcSessionExchangeRequest request,
            HttpServletResponse servletResponse) {
        LoginResponse response = sessionHandoffService.exchange(request.code());
        LoginResponse clientResponse =
                browserSessionCookieService.applyLoginSession(servletResponse, response);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("OIDC login successful", clientResponse));
    }

    private ResponseEntity<Void> redirect(URI destination) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(destination)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private URI frontendCompletionUri(String parameterName, String parameterValue) {
        return UriComponentsBuilder.fromUri(oidcLoginProperties.requireFrontendCompletionUri())
                .queryParam(parameterName, parameterValue)
                .build()
                .encode()
                .toUri();
    }
}
