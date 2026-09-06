package com.chacha.multitenantsaas.billing.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StripePlanCatalogProvisionerTest {

    @Test
    void paidPlanCreationCreatesProductAndRecurringPriceAndPersistsMapping() {
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
        StripePlanCatalogProvisioner provisioner =
                new StripePlanCatalogProvisioner(
                        properties(), mappingRepository, planRepository, builder);

        server.expect(requestTo("https://api.stripe.com/v1/products"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("name=Pro")))
                .andRespond(withSuccess("{\"id\":\"prod_managed\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.stripe.com/v1/prices"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("product=prod_managed")))
                .andExpect(content().string(containsString("currency=inr")))
                .andExpect(content().string(containsString("unit_amount=49900")))
                .andExpect(
                        content().string(containsString("recurring%5Binterval%5D=month")))
                .andRespond(withSuccess("{\"id\":\"price_managed\"}", MediaType.APPLICATION_JSON));

        provisioner.planCreated(plan(planId, "499.00", BillingInterval.MONTHLY, "Pro", null));

        ArgumentCaptor<SubscriptionPlanProviderMapping> mappingCaptor =
                ArgumentCaptor.forClass(SubscriptionPlanProviderMapping.class);
        verify(mappingRepository).saveAndFlush(mappingCaptor.capture());
        SubscriptionPlanProviderMapping mapping = mappingCaptor.getValue();
        assertThat(mapping.getProvider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(mapping.getEnvironment()).isEqualTo(BillingProviderEnvironment.TEST);
        assertThat(mapping.getProviderProductId()).isEqualTo("prod_managed");
        assertThat(mapping.getProviderPriceId()).isEqualTo("price_managed");
        assertThat(mapping.getStatus()).isEqualTo(SubscriptionPlanProviderMappingStatus.ACTIVE);
        server.verify();
    }

    @Test
    void commercialChangeCreatesReplacementPriceAndArchivesPreviousMapping() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan planEntity = mock(SubscriptionPlan.class);
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanRepository planRepository = mock(SubscriptionPlanRepository.class);
        SubscriptionPlanProviderMapping current =
                new SubscriptionPlanProviderMapping(
                        planEntity,
                        BillingProviderType.STRIPE,
                        BillingProviderEnvironment.TEST,
                        "prod_existing",
                        "price_old",
                        null);
        when(mappingRepository
                        .findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                                planId,
                                BillingProviderType.STRIPE,
                                BillingProviderEnvironment.TEST,
                                SubscriptionPlanProviderMappingStatus.ACTIVE))
                .thenReturn(Optional.of(current));
        when(planRepository.findById(planId)).thenReturn(Optional.of(planEntity));
        when(mappingRepository.saveAndFlush(any(SubscriptionPlanProviderMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripePlanCatalogProvisioner provisioner =
                new StripePlanCatalogProvisioner(
                        properties(), mappingRepository, planRepository, builder);

        server.expect(requestTo("https://api.stripe.com/v1/prices"))
                .andExpect(content().string(containsString("product=prod_existing")))
                .andExpect(content().string(containsString("unit_amount=79900")))
                .andRespond(withSuccess("{\"id\":\"price_new\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.stripe.com/v1/prices/price_old"))
                .andExpect(content().string(containsString("active=false")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        SubscriptionPlanResponse before =
                plan(planId, "499.00", BillingInterval.MONTHLY, "Pro", null);
        SubscriptionPlanResponse after =
                plan(planId, "799.00", BillingInterval.MONTHLY, "Pro", null);
        provisioner.planUpdated(before, after);

        assertThat(current.getStatus()).isEqualTo(SubscriptionPlanProviderMappingStatus.ARCHIVED);
        ArgumentCaptor<SubscriptionPlanProviderMapping> mappingCaptor =
                ArgumentCaptor.forClass(SubscriptionPlanProviderMapping.class);
        verify(mappingRepository).saveAndFlush(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getProviderPriceId()).isEqualTo("price_new");
        assertThat(mappingCaptor.getValue().getProviderProductId()).isEqualTo("prod_existing");
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

    private StripeBillingProperties properties() {
        StripeBillingProperties properties = new StripeBillingProperties();
        properties.setEnabled(true);
        properties.setEnvironment(BillingProviderEnvironment.TEST);
        properties.setSecretKey("sk_test_example");
        properties.setBaseUrl("https://api.stripe.com");
        properties.setSuccessUrl("https://app.example.com/billing/success");
        properties.setCancelUrl("https://app.example.com/billing/cancel");
        properties.setPrices(Map.of("pro", "price_legacy"));
        return properties;
    }
}
