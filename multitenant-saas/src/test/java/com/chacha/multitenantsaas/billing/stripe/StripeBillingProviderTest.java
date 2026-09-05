package com.chacha.multitenantsaas.billing.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StripeBillingProviderTest {

    @Test
    void springBeanFactorySelectsThePropertiesInjectionConstructor() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "stripe-test", Map.of("app.billing.stripe.enabled", "true")));
            context.registerBean(StripeBillingProperties.class, this::properties);
            context.register(StripeBillingProvider.class);
            context.refresh();

            StripeBillingProvider provider = context.getBean(StripeBillingProvider.class);
            assertThat(provider.providerType()).isEqualTo(BillingProviderType.STRIPE);
        }
    }

    @Test
    void createsSubscriptionCheckoutSessionWithTenantReconciliationData() {
        UUID tenantId = UUID.randomUUID();
        StripeBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripeBillingProvider provider = new StripeBillingProvider(properties, builder);

        server.expect(requestTo("https://api.stripe.com/v1/checkout/sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk_test_example"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("mode=subscription")))
                .andExpect(content().string(containsString("client_reference_id=" + tenantId)))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "subscription_data%5Bmetadata%5D%5Btenant_id%5D="
                                                        + tenantId)))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "subscription_data%5Bmetadata%5D%5Bplan_code%5D=PRO")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "line_items%5B0%5D%5Bprice%5D=price_pro_test")))
                .andRespond(
                        withSuccess(
                                """
                                {"id":"cs_test_123","url":"https://checkout.stripe.com/test"}
                                """,
                                MediaType.APPLICATION_JSON));

        BillingCheckoutSession session = provider.createCheckoutSession(tenantId, "pro");

        assertThat(session.sessionId()).isEqualTo("cs_test_123");
        assertThat(session.checkoutUrl()).isEqualTo("https://checkout.stripe.com/test");
        assertThat(session.provider()).isEqualTo(BillingProviderType.STRIPE);
        server.verify();
    }

    @Test
    void rejectsPlanWithoutConfiguredStripePrice() {
        StripeBillingProvider provider =
                new StripeBillingProvider(properties(), RestClient.builder());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.createCheckoutSession(UUID.randomUUID(), "enterprise"))
                .withMessage("No Stripe price is configured for plan: ENTERPRISE");
    }

    @Test
    void convertsStripeHttpFailureIntoProviderNeutralException() {
        StripeBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripeBillingProvider provider = new StripeBillingProvider(properties, builder);

        server.expect(requestTo("https://api.stripe.com/v1/checkout/sessions"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.createCheckoutSession(UUID.randomUUID(), "PRO"))
                .isInstanceOf(BillingProviderException.class)
                .hasMessage("Stripe checkout session creation failed");
        server.verify();
    }

    @Test
    void fetchesProviderSubscriptionSnapshot() {
        StripeBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripeBillingProvider provider = new StripeBillingProvider(properties, builder);

        server.expect(requestTo("https://api.stripe.com/v1/subscriptions/sub_test_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "sub_test_123",
                                  "status": "active",
                                  "cancel_at_period_end": true,
                                  "items": {
                                    "data": [{
                                      "current_period_start": 1787460000,
                                      "current_period_end": 1790138400,
                                      "price": {"id": "price_pro_test"}
                                    }]
                                  }
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        BillingProviderSubscriptionSnapshot snapshot = provider.fetchSubscription("sub_test_123");

        assertThat(snapshot.provider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(snapshot.providerSubscriptionId()).isEqualTo("sub_test_123");
        assertThat(snapshot.planCode()).isEqualTo("PRO");
        assertThat(snapshot.status()).isEqualTo(TenantSubscriptionStatus.ACTIVE);
        assertThat(snapshot.currentPeriodStart()).isEqualTo(Instant.ofEpochSecond(1787460000));
        assertThat(snapshot.currentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1790138400));
        assertThat(snapshot.cancelAtPeriodEnd()).isTrue();
        server.verify();
    }

    @Test
    void schedulesProviderSubscriptionCancellationAtPeriodEnd() {
        StripeBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripeBillingProvider provider = new StripeBillingProvider(properties, builder);

        server.expect(requestTo("https://api.stripe.com/v1/subscriptions/sub_test_123"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("cancel_at_period_end=true")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        provider.cancelSubscription("sub_test_123");

        server.verify();
    }

    private StripeBillingProperties properties() {
        StripeBillingProperties properties = new StripeBillingProperties();
        properties.setEnabled(true);
        properties.setSecretKey("sk_test_example");
        properties.setBaseUrl("https://api.stripe.com");
        properties.setSuccessUrl("https://app.example.com/billing/success");
        properties.setCancelUrl("https://app.example.com/billing/cancel");
        properties.setPrices(Map.of("pro", "price_pro_test"));
        return properties;
    }
}
