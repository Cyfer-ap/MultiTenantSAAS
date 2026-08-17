package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.config.BrowserSessionProperties;
import com.chacha.multitenantsaas.config.EmailVerificationProperties;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.entity.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class BrowserSessionCookieServiceTest {

    @Test
    void persistentLoginMovesRefreshCredentialToSecureHttpOnlyCookie() {
        BrowserSessionProperties properties = new BrowserSessionProperties();
        properties.setCookieMode(true);
        properties.setSecure(true);
        properties.setPartitioned(true);
        properties.setSameSite("None");

        EmailVerificationProperties emailProperties = new EmailVerificationProperties();
        emailProperties.setTrustedBrowserDays(30L);

        BrowserSessionCookieService service =
                new BrowserSessionCookieService(properties, emailProperties, 30L);

        LoginResponse internalResponse =
                new LoginResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Grace Hopper",
                        "grace@example.com",
                        UserRole.TENANT_ADMIN,
                        "access-token",
                        "refresh-secret",
                        "csrf-proof",
                        "Bearer",
                        900L,
                        true,
                        "Login successful");

        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        LoginResponse clientResponse = service.applyLoginSession(servletResponse, internalResponse);

        assertThat(clientResponse.refreshToken()).isNull();
        assertThat(clientResponse.csrfToken()).isEqualTo("csrf-proof");

        assertThat(servletResponse.getHeader("Set-Cookie"))
                .contains("mt_refresh=refresh-secret")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("Partitioned")
                .contains("SameSite=None")
                .contains("Max-Age=2592000");

        assertThat(servletResponse.getHeader("Cache-Control")).isEqualTo("no-store");
    }
}
