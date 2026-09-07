package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderCreateRequest;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.IdentityProviderSecretCipher;
import com.chacha.multitenantsaas.service.TenantIdentityProviderService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties =
                "app.identity-federation.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Transactional
class TenantIdentityProviderLifecycleIntegrationTest {

    @Autowired private TenantIdentityProviderService identityProviderService;
    @Autowired private TenantIdentityProviderRepository identityProviderRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private IdentityProviderSecretCipher secretCipher;

    @Test
    void createsUpdatesRotatesAndDisablesWithoutReturningPlaintextSecret() {
        Tenant tenant = createTenant("sso-labs");
        AppUser actor = createAdmin(tenant);
        String firstSecret = "first-client-secret-123";

        var created =
                identityProviderService.create(
                        tenant.getId(),
                        actor,
                        new TenantIdentityProviderCreateRequest(
                                "Corporate Login",
                                IdentityProviderProtocol.OIDC,
                                "https://8.8.8.8/tenant/v2.0",
                                "client-id-123",
                                firstSecret,
                                null));

        TenantIdentityProvider stored =
                identityProviderRepository.findByTenant_Id(tenant.getId()).orElseThrow();

        assertThat(created.status()).isEqualTo(TenantIdentityProviderStatus.DRAFT);
        assertThat(created.scopes()).containsExactlyInAnyOrder("openid", "profile", "email");
        assertThat(created.clientSecretHint()).startsWith("****");
        assertThat(created.secretVersion()).isEqualTo(1);
        assertThat(stored.getClientSecretCiphertext()).isNotEqualTo(firstSecret);
        assertThat(secretCipher.decrypt(stored.getClientSecretCiphertext())).isEqualTo(firstSecret);

        var updated =
                identityProviderService.update(
                        tenant.getId(),
                        actor,
                        new TenantIdentityProviderUpdateRequest(
                                "Corporate OIDC",
                                "https://8.8.4.4/oidc",
                                "client-id-456",
                                Set.of("openid", "email", "groups")));

        assertThat(updated.displayName()).isEqualTo("Corporate OIDC");
        assertThat(updated.issuerUri()).isEqualTo("https://8.8.4.4/oidc");
        assertThat(updated.scopes()).containsExactlyInAnyOrder("openid", "email", "groups");

        String rotatedSecret = "rotated-client-secret-456";
        var rotated =
                identityProviderService.rotateClientSecret(tenant.getId(), actor, rotatedSecret);
        TenantIdentityProvider afterRotation =
                identityProviderRepository.findByTenant_Id(tenant.getId()).orElseThrow();

        assertThat(rotated.secretVersion()).isEqualTo(2);
        assertThat(rotated.clientSecretHint()).startsWith("****");
        assertThat(secretCipher.decrypt(afterRotation.getClientSecretCiphertext()))
                .isEqualTo(rotatedSecret);

        var disabled = identityProviderService.disable(tenant.getId(), actor);
        var disabledAgain = identityProviderService.disable(tenant.getId(), actor);

        assertThat(disabled.status()).isEqualTo(TenantIdentityProviderStatus.DISABLED);
        assertThat(disabled.disabledAt()).isNotNull();
        assertThat(disabledAgain.status()).isEqualTo(TenantIdentityProviderStatus.DISABLED);
        assertThat(identityProviderService.get(tenant.getId()).clientSecretHint())
                .isEqualTo(rotated.clientSecretHint());
    }

    @Test
    void preventsDuplicateConfigurationAndCrossTenantReads() {
        Tenant ownerTenant = createTenant("sso-owner");
        AppUser owner = createAdmin(ownerTenant);
        Tenant otherTenant = createTenant("sso-other");

        identityProviderService.create(
                ownerTenant.getId(),
                owner,
                new TenantIdentityProviderCreateRequest(
                        "Owner OIDC",
                        IdentityProviderProtocol.OIDC,
                        "https://8.8.8.8/oidc",
                        "owner-client",
                        "owner-client-secret",
                        Set.of("openid", "email")));

        assertThatThrownBy(
                        () ->
                                identityProviderService.create(
                                        ownerTenant.getId(),
                                        owner,
                                        new TenantIdentityProviderCreateRequest(
                                                "Duplicate OIDC",
                                                IdentityProviderProtocol.OIDC,
                                                "https://8.8.4.4/oidc",
                                                "duplicate-client",
                                                "duplicate-client-secret",
                                                Set.of("openid"))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        assertThatThrownBy(() -> identityProviderService.get(otherTenant.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tenant identity-provider configuration not found");
    }

    @Test
    void rejectsScopesWithoutOpenid() {
        Tenant tenant = createTenant("sso-scope");
        AppUser actor = createAdmin(tenant);

        assertThatThrownBy(
                        () ->
                                identityProviderService.create(
                                        tenant.getId(),
                                        actor,
                                        new TenantIdentityProviderCreateRequest(
                                                "Invalid scopes",
                                                IdentityProviderProtocol.OIDC,
                                                "https://8.8.8.8/oidc",
                                                "scope-client",
                                                "scope-client-secret",
                                                Set.of("profile", "email"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OIDC scopes must include openid");
    }

    private Tenant createTenant(String prefix) {
        return tenantRepository.saveAndFlush(
                new Tenant(
                        "SSO Labs", prefix + "-" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private AppUser createAdmin(Tenant tenant) {
        return appUserRepository.saveAndFlush(
                new AppUser(
                        tenant,
                        "SSO Admin",
                        "admin-" + UUID.randomUUID() + "@example.com",
                        "not-used",
                        UserRole.TENANT_ADMIN));
    }
}
