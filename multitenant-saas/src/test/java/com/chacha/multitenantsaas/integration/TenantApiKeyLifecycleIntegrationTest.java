package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.dto.TenantApiKeyCreateRequest;
import com.chacha.multitenantsaas.dto.TenantApiKeyCreatedResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantApiKey;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantApiKeyRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.SecureTokenService;
import com.chacha.multitenantsaas.service.TenantApiKeyService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantApiKeyLifecycleIntegrationTest {

    @Autowired private TenantApiKeyService apiKeyService;

    @Autowired private TenantApiKeyRepository apiKeyRepository;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private SecureTokenService secureTokenService;

    @Test
    void createsOneTimeSecretStoresOnlyHashAndRevokesIdempotently() {
        Tenant tenant = createTenant("key-labs");
        AppUser actor = createAdmin(tenant);

        TenantApiKeyCreatedResponse created =
                apiKeyService.create(
                        tenant.getId(),
                        actor,
                        new TenantApiKeyCreateRequest(
                                " Deployment key ", Instant.now().plus(30, ChronoUnit.DAYS)));

        TenantApiKey stored = apiKeyRepository.findById(created.id()).orElseThrow();

        assertThat(created.apiKey()).startsWith("mts_");
        assertThat(created.apiKey()).startsWith(created.keyPrefix());
        assertThat(created.name()).isEqualTo("Deployment key");
        assertThat(stored.getKeyHash()).isNotEqualTo(created.apiKey());
        assertThat(stored.getKeyHash()).isEqualTo(secureTokenService.hashToken(created.apiKey()));
        assertThat(secureTokenService.matchesToken(created.apiKey(), stored.getKeyHash())).isTrue();
        assertThat(secureTokenService.matchesToken(created.apiKey() + "x", stored.getKeyHash()))
                .isFalse();
        assertThat(apiKeyService.list(tenant.getId()))
                .singleElement()
                .satisfies(
                        key -> {
                            assertThat(key.id()).isEqualTo(created.id());
                            assertThat(key.active()).isTrue();
                        });

        var revoked = apiKeyService.revoke(tenant.getId(), created.id(), actor);
        Instant firstRevokedAt = revoked.revokedAt();
        var duplicateRevoke = apiKeyService.revoke(tenant.getId(), created.id(), actor);

        assertThat(revoked.active()).isFalse();
        assertThat(firstRevokedAt).isNotNull();
        assertThat(duplicateRevoke.revokedAt()).isEqualTo(firstRevokedAt);
    }

    @Test
    void preventsCrossTenantRevocation() {
        Tenant ownerTenant = createTenant("owner");
        AppUser owner = createAdmin(ownerTenant);
        Tenant otherTenant = createTenant("other");
        AppUser otherAdmin = createAdmin(otherTenant);
        TenantApiKeyCreatedResponse created =
                apiKeyService.create(
                        ownerTenant.getId(),
                        owner,
                        new TenantApiKeyCreateRequest("Owner key", null));

        assertThatThrownBy(
                        () -> apiKeyService.revoke(otherTenant.getId(), created.id(), otherAdmin))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("API key not found");

        assertThat(apiKeyRepository.findById(created.id()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void rejectsExpiredExpiryAtServiceBoundary() {
        Tenant tenant = createTenant("expired");
        AppUser actor = createAdmin(tenant);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                apiKeyService.create(
                                        tenant.getId(),
                                        actor,
                                        new TenantApiKeyCreateRequest(
                                                "Expired key", Instant.now().minusSeconds(1))))
                .withMessage("API key expiry must be in the future");
    }

    private Tenant createTenant(String prefix) {
        return tenantRepository.saveAndFlush(
                new Tenant(
                        "API Key Labs",
                        prefix + "-" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private AppUser createAdmin(Tenant tenant) {
        return appUserRepository.saveAndFlush(
                new AppUser(
                        tenant,
                        "API Key Admin",
                        "admin-" + UUID.randomUUID() + "@example.com",
                        "not-used",
                        UserRole.TENANT_ADMIN));
    }
}
