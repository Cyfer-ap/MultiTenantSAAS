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
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StripeBillingProviderTest {

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
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("mode=subscription")))
                .andExpect(content().string(containsString("client_reference_id=" + tenantId)))
                .andExpect(content().string(containsString("price=price_pro_test")))
                .andRespond(
                        withSuccess(
                                "{"id":"cs_test_123","url":"https://checkout.stripe.com/test"}",
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
                .isThrownBy(
                        () -> provider.createCheckoutSession(UUID.randomUUID(), "enterprise"))
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
    void cancelsProviderSubscription() {
        StripeBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StripeBillingProvider provider = new StripeBillingProvider(properties, builder);

        server.expect(requestTo("https://api.stripe.com/v1/subscriptions/sub_test_123"))
                .andExpect(method(HttpMethod.DELETE))
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
        properties.setPrices(Map.of("PRO", "price_pro_test"));
        return properties;
    }
}
