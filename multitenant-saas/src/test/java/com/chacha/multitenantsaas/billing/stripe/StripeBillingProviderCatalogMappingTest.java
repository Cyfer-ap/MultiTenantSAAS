package com.chacha.multitenantsaas.billing.stripe;

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
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanProviderMappingRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StripeBillingProviderCatalogMappingTest {

    @Test
    void checkoutPrefersDurableCatalogMappingOverLegacyConfiguration() {
        StripeBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripeBillingProvider provider = new StripeBillingProvider(properties, builder);
        SubscriptionPlanProviderMappingRepository repository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        SubscriptionPlanProviderMapping mapping = mock(SubscriptionPlanProviderMapping.class);
        when(mapping.getProviderPriceId()).thenReturn("price_database");
        when(repository
                        .findFirstByPlan_CodeIgnoreCaseAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                                "PRO",
                                BillingProviderType.STRIPE,
                                BillingProviderEnvironment.TEST,
                                SubscriptionPlanProviderMappingStatus.ACTIVE))
                .thenReturn(Optional.of(mapping));
        provider.setMappingRepository(repository);

        server.expect(requestTo("https://api.stripe.com/v1/checkout/sessions"))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "line_items%5B0%5D%5Bprice%5D=price_database")))
                .andRespond(
                        withSuccess(
                                """
                                {"id":"cs_test_db","url":"https://checkout.stripe.com/db"}
                                """,
                                MediaType.APPLICATION_JSON));

        var session = provider.createCheckoutSession(UUID.randomUUID(), "PRO");

        assertThat(session.sessionId()).isEqualTo("cs_test_db");
        server.verify();
    }

    @Test
    void historicalSubscriptionCanResolveArchivedDatabasePrice() {
        StripeBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripeBillingProvider provider = new StripeBillingProvider(properties, builder);
        SubscriptionPlanProviderMappingRepository repository =
                mock(SubscriptionPlanProviderMappingRepository.class);
        when(repository.findPlanCodeByProviderPriceId(
                        BillingProviderType.STRIPE,
                        BillingProviderEnvironment.TEST,
                        "price_retired"))
                .thenReturn(Optional.of("PRO"));
        provider.setMappingRepository(repository);

        server.expect(requestTo("https://api.stripe.com/v1/subscriptions/sub_history"))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id":"sub_history",
                                  "status":"active",
                                  "current_period_start":1787460000,
                                  "current_period_end":1790138400,
                                  "items":{"data":[{"price":{"id":"price_retired"}}]}
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        BillingProviderSubscriptionSnapshot snapshot = provider.fetchSubscription("sub_history");

        assertThat(snapshot.planCode()).isEqualTo("PRO");
        server.verify();
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
