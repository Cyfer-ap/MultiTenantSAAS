package com.chacha.multitenantsaas.billing.razorpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chacha.multitenantsaas.billing.entity.BillingProviderEnvironment;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMapping;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMappingStatus;
import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanProviderMappingRepository;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayBillingProviderCatalogMappingTest {

    @Test
    void checkoutPrefersActiveManagedPlanMappingOverLegacyConfiguration() {
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlan plan = mock(SubscriptionPlan.class);
        SubscriptionPlanProviderMapping mapping =
                new SubscriptionPlanProviderMapping(
                        plan,
                        BillingProviderType.RAZORPAY,
                        BillingProviderEnvironment.TEST,
                        null,
                        null,
                        "plan_managed");
        when(mappingRepository
                        .findFirstByPlan_CodeIgnoreCaseAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                                "PRO",
                                BillingProviderType.RAZORPAY,
                                BillingProviderEnvironment.TEST,
                                SubscriptionPlanProviderMappingStatus.ACTIVE))
                .thenReturn(Optional.of(mapping));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayBillingProvider provider = new RazorpayBillingProvider(properties(), builder);
        provider.setMappingRepository(mappingRepository);
        UUID tenantId = UUID.randomUUID();

        server.expect(requestTo("https://api.razorpay.com/v1/subscriptions"))
                .andExpect(content().string(containsString("\"plan_id\":\"plan_managed\"")))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "sub_managed",
                                  "short_url": "https://rzp.io/i/managed"
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        BillingCheckoutSession session = provider.createCheckoutSession(tenantId, "pro");

        assertThat(session.sessionId()).isEqualTo("sub_managed");
        server.verify();
    }

    @Test
    void subscriptionLookupResolvesArchivedManagedPlanBackToLocalCode() {
        SubscriptionPlanProviderMappingRepository mappingRepository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        when(mappingRepository.findPlanCodeByProviderPlanId(
                        BillingProviderType.RAZORPAY,
                        BillingProviderEnvironment.TEST,
                        "plan_archived_version"))
                .thenReturn(Optional.of("growth"));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayBillingProvider provider = new RazorpayBillingProvider(properties(), builder);
        provider.setMappingRepository(mappingRepository);

        server.expect(requestTo("https://api.razorpay.com/v1/subscriptions/sub_managed"))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "sub_managed",
                                  "plan_id": "plan_archived_version",
                                  "status": "active",
                                  "current_start": 1787460000,
                                  "current_end": 1790138400,
                                  "cancel_at_cycle_end": false
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        BillingProviderSubscriptionSnapshot snapshot = provider.fetchSubscription("sub_managed");

        assertThat(snapshot.planCode()).isEqualTo("GROWTH");
        server.verify();
    }

    private RazorpayBillingProperties properties() {
        RazorpayBillingProperties properties = new RazorpayBillingProperties();
        properties.setEnabled(true);
        properties.setEnvironment(BillingProviderEnvironment.TEST);
        properties.setKeyId("rzp_test_key");
        properties.setKeySecret("test_secret");
        properties.setBaseUrl("https://api.razorpay.com");
        properties.setTotalCount(120);
        properties.setPlans(Map.of("pro", "plan_legacy"));
        return properties;
    }
}
