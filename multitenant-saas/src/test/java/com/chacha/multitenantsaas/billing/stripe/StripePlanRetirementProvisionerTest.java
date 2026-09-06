package com.chacha.multitenantsaas.billing.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.repository.SubscriptionPlanRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StripePlanRetirementProvisionerTest {

    @Test
    void retirementDeactivatesCurrentPriceAndProductAndArchivesMapping() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan planEntity = mock(SubscriptionPlan.class);
        SubscriptionPlanProviderMapping mapping =
                new SubscriptionPlanProviderMapping(
                        planEntity,
                        BillingProviderType.STRIPE,
                        BillingProviderEnvironment.TEST,
                        "prod_test",
                        "price_test",
                        null);
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanRepository planRepository = mock(SubscriptionPlanRepository.class);
        when(mappingRepository
                        .findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                                planId,
                                BillingProviderType.STRIPE,
                                BillingProviderEnvironment.TEST,
                                SubscriptionPlanProviderMappingStatus.ACTIVE))
                .thenReturn(Optional.of(mapping));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripePlanRetirementProvisioner provisioner =
                new StripePlanRetirementProvisioner(
                        properties(), mappingRepository, planRepository, builder);

        server.expect(requestTo("https://api.stripe.com/v1/prices/price_test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("active=false")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.stripe.com/v1/products/prod_test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("active=false")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        SubscriptionPlanResponse plan = mock(SubscriptionPlanResponse.class);
        when(plan.id()).thenReturn(planId);
        when(plan.code()).thenReturn("PRO");
        provisioner.planRetired(plan);

        assertThat(mapping.getStatus()).isEqualTo(SubscriptionPlanProviderMappingStatus.ARCHIVED);
        verify(mappingRepository).saveAndFlush(mapping);
        server.verify();
    }

    private StripeBillingProperties properties() {
        StripeBillingProperties properties = new StripeBillingProperties();
        properties.setEnabled(true);
        properties.setEnvironment(BillingProviderEnvironment.TEST);
        properties.setSecretKey("sk_test_example");
        properties.setBaseUrl("https://api.stripe.com");
        properties.setPrices(Map.of("pro", "price_legacy"));
        return properties;
    }
}
