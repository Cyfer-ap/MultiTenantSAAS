package com.chacha.multitenantsaas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.config.OidcLoginProperties;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.service.BrowserSessionCookieService;
import com.chacha.multitenantsaas.service.OidcAuthorizationService;
import com.chacha.multitenantsaas.service.OidcCallbackService;
import com.chacha.multitenantsaas.service.OidcSessionHandoffService;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OidcAuthenticationControllerTest {

    @Mock private OidcAuthorizationService authorizationService;
    @Mock private OidcCallbackService callbackService;
    @Mock private OidcSessionHandoffService sessionHandoffService;
    @Mock private BrowserSessionCookieService browserSessionCookieService;

    private OidcAuthenticationController controller;

    @BeforeEach
    void setUp() {
        OidcLoginProperties properties = new OidcLoginProperties();
        properties.setFrontendCompletionUri("http://localhost:8080/auth/oidc/complete");
        controller =
                new OidcAuthenticationController(
                        authorizationService,
                        callbackService,
                        sessionHandoffService,
                        browserSessionCookieService,
                        properties);
    }

    @Test
    void callbackRedirectsWithOpaqueHandoffAndNeverApplicationTokens() {
        when(callbackService.authenticate("state-value", "provider-code", null))
                .thenReturn(
                        new OidcSessionHandoffService.IssuedHandoff(
                                "opaque-handoff", Instant.now().plusSeconds(60)));

        ResponseEntity<Void> response =
                controller.callback("state-value", "provider-code", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        URI location = response.getHeaders().getLocation();
        assertThat(location)
                .isEqualTo(URI.create("http://localhost:8080/auth/oidc/complete?code=opaque-handoff"));
        assertThat(location.toString())
                .doesNotContain("accessToken", "refreshToken", "csrfToken", "Bearer");
    }

    @Test
    void authenticationFailureReturnsCleanFrontendErrorRedirect() {
        when(callbackService.authenticate("state-value", "provider-code", "access_denied"))
                .thenThrow(new AuthenticationFailedException("OIDC authentication failed"));

        ResponseEntity<Void> response =
                controller.callback("state-value", "provider-code", "access_denied");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(
                        URI.create(
                                "http://localhost:8080/auth/oidc/complete?error=authentication_failed"));
    }
}
