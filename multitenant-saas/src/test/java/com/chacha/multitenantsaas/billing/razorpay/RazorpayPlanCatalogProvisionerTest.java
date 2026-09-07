package com.chacha.multitenantsaas.billing.razorpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chacha.multitenantsaas.billing.entity.BillingProviderEnvironment;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMapping;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMappingStatus;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanProviderMappingRepository;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.repository.SubscriptionPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayPlanCatalogProvisionerTest {

    @Test
    void springBeanFactorySelectsProductionConstructor() {
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanRepository planRepository = mock(SubscriptionPlanRepository.class);

        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "razorpay-plan-test",
                                    Map.of("app.billing.razorpay.enabled", "true")));
            context.registerBean(RazorpayBillingProperties.class, this::properties);
            context.registerBean(
                    SubscriptionPlanProviderMappingRepository.class, () -> mappingRepository);
            context.registerBean(SubscriptionPlanRepository.class, () -> planRepository);
            context.register(RazorpayPlanCatalogProvisioner.class);
            context.refresh();

            assertThat(context.getBean(RazorpayPlanCatalogProvisioner.class)).isNotNull();
        }
    }

    @Test
    void paidPlanCreationCreatesRazorpayPlanAndPersistsManagedMapping() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan planEntity = mock(SubscriptionPlan.class);
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanRepository planRepository = mock(SubscriptionPlanRepository.class);
        when(planRepository.findById(planId)).thenReturn(Optional.of(planEntity));
        when(mappingRepository.saveAndFlush(any(SubscriptionPlanProviderMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayPlanCatalogProvisioner provisioner =
                new RazorpayPlanCatalogProvisioner(
                        properties(), mappingRepository, planRepository, builder);

        server.expect(requestTo("https://api.razorpay.com/v1/plans?count=100&skip=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/plans"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"period\":\"monthly\"")))
                .andExpect(content().string(containsString("\"amount\":49900")))
                .andExpect(content().string(containsString("\"currency\":\"INR\"")))
                .andExpect(content().string(containsString("\"mtsaas_local_plan_id\":\"" + planId + "\"")))
                .andRespond(
                        withSuccess(
                                "{\"id\":\"plan_managed\",\"entity\":\"plan\"}",
                                MediaType.APPLICATION_JSON));

        provisioner.planCreated(plan(planId, "499.00", BillingInterval.MONTHLY, "Pro", null));

        ArgumentCaptor<SubscriptionPlanProviderMapping> mappingCaptor =
                ArgumentCaptor.forClass(SubscriptionPlanProviderMapping.class);
        verify(mappingRepository).saveAndFlush(mappingCaptor.capture());
        SubscriptionPlanProviderMapping mapping = mappingCaptor.getValue();
        assertThat(mapping.getProvider()).isEqualTo(BillingProviderType.RAZORPAY);
        assertThat(mapping.getEnvironment()).isEqualTo(BillingProviderEnvironment.TEST);
        assertThat(mapping.getProviderPlanId()).isEqualTo("plan_managed");
        assertThat(mapping.getProviderProductId()).isNull();
        assertThat(mapping.getProviderPriceId()).isNull();
        assertThat(mapping.getStatus()).isEqualTo(SubscriptionPlanProviderMappingStatus.ACTIVE);
        server.verify();
    }

    @Test
    void providerVisibleChangeCreatesReplacementAndArchivesOldLocalMapping() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan planEntity = mock(SubscriptionPlan.class);
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanRepository planRepository = mock(SubscriptionPlanRepository.class);
        SubscriptionPlanProviderMapping current =
                new SubscriptionPlanProviderMapping(
                        planEntity,
                        BillingProviderType.RAZORPAY,
                        BillingProviderEnvironment.TEST,
                        null,
                        null,
                        "plan_old");
        when(mappingRepository
                        .findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                                planId,
                                BillingProviderType.RAZORPAY,
                                BillingProviderEnvironment.TEST,
                                SubscriptionPlanProviderMappingStatus.ACTIVE))
                .thenReturn(Optional.of(current));
        when(planRepository.findById(planId)).thenReturn(Optional.of(planEntity));
        when(mappingRepository.saveAndFlush(any(SubscriptionPlanProviderMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayPlanCatalogProvisioner provisioner =
                new RazorpayPlanCatalogProvisioner(
                        properties(), mappingRepository, planRepository, builder);

        server.expect(requestTo("https://api.razorpay.com/v1/plans?count=100&skip=0"))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/plans"))
                .andExpect(content().string(containsString("\"amount\":79900")))
                .andRespond(withSuccess("{\"id\":\"plan_new\"}", MediaType.APPLICATION_JSON));

        SubscriptionPlanResponse before =
                plan(planId, "499.00", BillingInterval.MONTHLY, "Pro", "Old terms");
        SubscriptionPlanResponse after =
                plan(planId, "799.00", BillingInterval.MONTHLY, "Pro Plus", "New terms");
        provisioner.planUpdated(before, after);

        assertThat(current.getStatus()).isEqualTo(SubscriptionPlanProviderMappingStatus.ARCHIVED);
        ArgumentCaptor<SubscriptionPlanProviderMapping> mappingCaptor =
                ArgumentCaptor.forClass(SubscriptionPlanProviderMapping.class);
        verify(mappingRepository, times(2)).saveAndFlush(mappingCaptor.capture());
        assertThat(mappingCaptor.getAllValues().getFirst().getProviderPlanId())
                .isEqualTo("plan_new");
        server.verify();
    }

    @Test
    void retryAdoptsMatchingManagedPlanInsteadOfCreatingDuplicate() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan planEntity = mock(SubscriptionPlan.class);
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanRepository planRepository = mock(SubscriptionPlanRepository.class);
        when(planRepository.findById(planId)).thenReturn(Optional.of(planEntity));
        when(mappingRepository.saveAndFlush(any(SubscriptionPlanProviderMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayPlanCatalogProvisioner provisioner =
                new RazorpayPlanCatalogProvisioner(
                        properties(), mappingRepository, planRepository, builder);
        SubscriptionPlanResponse plan =
                plan(planId, "499.00", BillingInterval.MONTHLY, "Pro", null);
        String fingerprint = fingerprint(plan);

        server.expect(requestTo("https://api.razorpay.com/v1/plans?count=100&skip=0"))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "items": [
                                    {
                                      "id": "plan_existing_managed",
                                      "notes": {"mtsaas_fingerprint": "%s"}
                                    }
                                  ]
                                }
                                """
                                        .formatted(fingerprint),
                                MediaType.APPLICATION_JSON));

        provisioner.planCreated(plan);

        ArgumentCaptor<SubscriptionPlanProviderMapping> mappingCaptor =
                ArgumentCaptor.forClass(SubscriptionPlanProviderMapping.class);
        verify(mappingRepository).saveAndFlush(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getProviderPlanId())
                .isEqualTo("plan_existing_managed");
        server.verify();
    }

    @Test
    void retirementArchivesLegacyMappingAfterProviderValidation() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan planEntity = mock(SubscriptionPlan.class);
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanRepository planRepository = mock(SubscriptionPlanRepository.class);
        when(mappingRepository
                        .findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                                planId,
                                BillingProviderType.RAZORPAY,
                                BillingProviderEnvironment.TEST,
                                SubscriptionPlanProviderMappingStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(mappingRepository.findAllByPlan_IdAndProviderAndEnvironmentOrderByCreatedAtDesc(
                        planId, BillingProviderType.RAZORPAY, BillingProviderEnvironment.TEST))
                .thenReturn(List.of());
        when(mappingRepository.findFirstByProviderAndEnvironmentAndProviderPlanId(
                        BillingProviderType.RAZORPAY,
                        BillingProviderEnvironment.TEST,
                        "plan_pro_legacy"))
                .thenReturn(Optional.empty());
        when(planRepository.findById(planId)).thenReturn(Optional.of(planEntity));
        when(mappingRepository.saveAndFlush(any(SubscriptionPlanProviderMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayPlanCatalogProvisioner provisioner =
                new RazorpayPlanCatalogProvisioner(
                        properties(), mappingRepository, planRepository, builder);

        server.expect(requestTo("https://api.razorpay.com/v1/plans/plan_pro_legacy"))
                .andRespond(withSuccess("{\"id\":\"plan_pro_legacy\"}", MediaType.APPLICATION_JSON));

        provisioner.planRetired(plan(planId, "499.00", BillingInterval.MONTHLY, "Pro", null));

        ArgumentCaptor<SubscriptionPlanProviderMapping> mappingCaptor =
                ArgumentCaptor.forClass(SubscriptionPlanProviderMapping.class);
        verify(mappingRepository, times(2)).saveAndFlush(mappingCaptor.capture());
        SubscriptionPlanProviderMapping imported = mappingCaptor.getAllValues().getFirst();
        assertThat(imported.getProviderPlanId()).isEqualTo("plan_pro_legacy");
        assertThat(imported.getStatus()).isEqualTo(SubscriptionPlanProviderMappingStatus.ARCHIVED);
        server.verify();
    }

    private SubscriptionPlanResponse plan(
            UUID id, String price, BillingInterval interval, String name, String description) {
        Instant now = Instant.parse("2026-09-07T00:00:00Z");
        return new SubscriptionPlanResponse(
                id,
                "PRO",
                name,
                description,
                interval,
                new BigDecimal(price),
                "INR",
                25,
                100,
                10240L,
                SubscriptionPlanStatus.ACTIVE,
                now,
                now);
    }

    private String fingerprint(SubscriptionPlanResponse plan) {
        String material =
                plan.id()
                        + "|"
                        + plan.name()
                        + "|"
                        + (plan.description() == null ? "" : plan.description())
                        + "|"
                        + plan.price().toPlainString()
                        + "|"
                        + plan.currency().toUpperCase()
                        + "|"
                        + plan.billingInterval();
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash, 0, 16);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private RazorpayBillingProperties properties() {
        RazorpayBillingProperties properties = new RazorpayBillingProperties();
        properties.setEnabled(true);
        properties.setEnvironment(BillingProviderEnvironment.TEST);
        properties.setKeyId("rzp_test_key");
        properties.setKeySecret("test_secret");
        properties.setBaseUrl("https://api.razorpay.com");
        properties.setTotalCount(120);
        properties.setPlans(Map.of("pro", "plan_pro_legacy"));
        return properties;
    }
}
