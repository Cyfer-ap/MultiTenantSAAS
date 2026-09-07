package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointCreateRequest;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OutboundWebhookEndpoint;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.OutboundWebhookEndpointRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.OutboundWebhookEndpointService;
import com.chacha.multitenantsaas.service.OutboundWebhookSecretCipher;
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
                "app.outbound-webhooks.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Transactional
class OutboundWebhookEndpointLifecycleIntegrationTest {

    @Autowired private OutboundWebhookEndpointService endpointService;
    @Autowired private OutboundWebhookEndpointRepository endpointRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private OutboundWebhookSecretCipher secretCipher;

    @Test
    void createsUpdatesRotatesAndArchivesWithoutRevealingStoredSecret() {
        Tenant tenant = createTenant("webhook-labs");
        AppUser actor = createAdmin(tenant);

        var created =
                endpointService.create(
                        tenant.getId(),
                        actor,
                        new OutboundWebhookEndpointCreateRequest(
                                " Deployment Events ",
                                "https://8.8.8.8/hooks",
                                true,
                                Set.of(
                                        OutboundWebhookEventType.PROJECT_CREATED,
                                        OutboundWebhookEventType.TASK_COMPLETED)));

        OutboundWebhookEndpoint stored =
                endpointRepository.findById(created.endpoint().id()).orElseThrow();
        String firstSecret = created.signingSecret();

        assertThat(firstSecret).startsWith("mwh_");
        assertThat(stored.getSecretCiphertext()).isNotEqualTo(firstSecret);
        assertThat(secretCipher.decrypt(stored.getSecretCiphertext())).isEqualTo(firstSecret);
        assertThat(created.endpoint().name()).isEqualTo("Deployment Events");
        assertThat(created.endpoint().secretVersion()).isEqualTo(1);
        assertThat(endpointService.list(tenant.getId())).hasSize(1);

        var updated =
                endpointService.update(
                        tenant.getId(),
                        stored.getId(),
                        actor,
                        new OutboundWebhookEndpointUpdateRequest(
                                "Deployment Events v2",
                                "https://8.8.4.4/webhooks/events",
                                false,
                                Set.of(OutboundWebhookEventType.SUBSCRIPTION_UPDATED)));

        assertThat(updated.enabled()).isFalse();
        assertThat(updated.url()).isEqualTo("https://8.8.4.4/webhooks/events");
        assertThat(updated.events()).containsExactly(OutboundWebhookEventType.SUBSCRIPTION_UPDATED);

        var rotated = endpointService.rotateSecret(tenant.getId(), stored.getId(), actor);
        OutboundWebhookEndpoint afterRotation =
                endpointRepository.findById(stored.getId()).orElseThrow();

        assertThat(rotated.signingSecret()).startsWith("mwh_").isNotEqualTo(firstSecret);
        assertThat(rotated.secretVersion()).isEqualTo(2);
        assertThat(secretCipher.decrypt(afterRotation.getSecretCiphertext()))
                .isEqualTo(rotated.signingSecret());

        var archived = endpointService.archive(tenant.getId(), stored.getId(), actor);
        var archivedAgain = endpointService.archive(tenant.getId(), stored.getId(), actor);

        assertThat(archived.enabled()).isFalse();
        assertThat(archivedAgain.id()).isEqualTo(archived.id());
        assertThat(endpointService.list(tenant.getId())).isEmpty();
        assertThatThrownBy(() -> endpointService.get(tenant.getId(), stored.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Outbound webhook endpoint not found");
    }

    @Test
    void preventsCrossTenantEndpointAccess() {
        Tenant ownerTenant = createTenant("owner-webhook");
        AppUser owner = createAdmin(ownerTenant);
        Tenant otherTenant = createTenant("other-webhook");
        AppUser otherAdmin = createAdmin(otherTenant);
        var created =
                endpointService.create(
                        ownerTenant.getId(),
                        owner,
                        new OutboundWebhookEndpointCreateRequest(
                                "Owner endpoint",
                                "https://8.8.8.8/hooks",
                                true,
                                Set.of(OutboundWebhookEventType.PROJECT_CREATED)));

        assertThatThrownBy(
                        () ->
                                endpointService.update(
                                        otherTenant.getId(),
                                        created.endpoint().id(),
                                        otherAdmin,
                                        new OutboundWebhookEndpointUpdateRequest(
                                                "Other endpoint",
                                                "https://8.8.4.4/hooks",
                                                true,
                                                Set.of(OutboundWebhookEventType.PROJECT_UPDATED))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Outbound webhook endpoint not found");
    }

    private Tenant createTenant(String prefix) {
        return tenantRepository.saveAndFlush(
                new Tenant(
                        "Webhook Labs",
                        prefix + "-" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private AppUser createAdmin(Tenant tenant) {
        return appUserRepository.saveAndFlush(
                new AppUser(
                        tenant,
                        "Webhook Admin",
                        "admin-" + UUID.randomUUID() + "@example.com",
                        "not-used",
                        UserRole.TENANT_ADMIN));
    }
}
