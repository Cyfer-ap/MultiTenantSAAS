package com.chacha.multitenantsaas.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantIdentityProviderSsoModeTest {

    private TenantIdentityProvider identityProvider;
    private AppUser actor;
    private Instant now;

    @BeforeEach
    void setUp() {
        actor = mock(AppUser.class);
        now = Instant.parse("2026-09-08T00:00:00Z");
        identityProvider =
                new TenantIdentityProvider(
                        mock(Tenant.class),
                        IdentityProviderProtocol.OIDC,
                        "Corporate SSO",
                        "https://idp.example.com",
                        "client-id",
                        "ciphertext",
                        "****secret",
                        Set.of("openid", "profile", "email"),
                        actor,
                        now);
    }

    @Test
    void startsOptionalAndCannotRequireSsoBeforeVerification() {
        assertThat(identityProvider.getSsoMode()).isEqualTo(TenantSsoMode.OPTIONAL);

        assertThatThrownBy(
                        () ->
                                identityProvider.updateSsoMode(
                                        TenantSsoMode.REQUIRED, actor, now.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("verified");
    }

    @Test
    void providerConfigurationChangeDropsRequiredSsoBackToOptional() {
        enableRequiredSso();

        identityProvider.update(
                "Corporate SSO v2",
                "https://idp.example.com",
                "client-id-2",
                Set.of("openid", "email"),
                actor,
                now.plusSeconds(3));

        assertThat(identityProvider.getStatus()).isEqualTo(TenantIdentityProviderStatus.DRAFT);
        assertThat(identityProvider.getSsoMode()).isEqualTo(TenantSsoMode.OPTIONAL);
        assertThat(identityProvider.getVerifiedAt()).isNull();
    }

    @Test
    void clientSecretRotationDropsRequiredSsoBackToOptional() {
        enableRequiredSso();

        identityProvider.rotateClientSecret("new-ciphertext", "****new", actor, now.plusSeconds(3));

        assertThat(identityProvider.getStatus()).isEqualTo(TenantIdentityProviderStatus.DRAFT);
        assertThat(identityProvider.getSsoMode()).isEqualTo(TenantSsoMode.OPTIONAL);
    }

    @Test
    void disablingProviderAlwaysDropsRequiredSsoBackToOptional() {
        enableRequiredSso();

        identityProvider.disable(actor, now.plusSeconds(3));

        assertThat(identityProvider.getStatus()).isEqualTo(TenantIdentityProviderStatus.DISABLED);
        assertThat(identityProvider.getSsoMode()).isEqualTo(TenantSsoMode.OPTIONAL);
    }

    private void enableRequiredSso() {
        identityProvider.markVerified(actor, now.plusSeconds(1));
        identityProvider.updateSsoMode(TenantSsoMode.REQUIRED, actor, now.plusSeconds(2));
        assertThat(identityProvider.getSsoMode()).isEqualTo(TenantSsoMode.REQUIRED);
    }
}
