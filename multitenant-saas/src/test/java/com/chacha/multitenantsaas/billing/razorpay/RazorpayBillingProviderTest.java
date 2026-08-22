package com.chacha.multitenantsaas.billing.razorpay;

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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayBillingProviderTest {

    @Test
    void createsSubscriptionLinkWithTenantReconciliationNotes() {
        UUID tenantId = UUID.randomUUID();
        RazorpayBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayBillingProvider provider = new RazorpayBillingProvider(properties, builder);

        String credentials =
                Base64.getEncoder()
                        .encodeToString(
                                "rzp_test_key:test_secret".getBytes(StandardCharsets.UTF_8));

        server.expect(requestTo("https://api.razorpay.com/v1/subscriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic " + credentials))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"plan_id\":\"plan_pro_test\"")))
                .andExpect(content().string(containsString("\"total_count\":120")))
                .andExpect(content().string(containsString("\"tenant_id\":\"" + tenantId + "\"")))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "sub_test_123",
                                  "short_url": "https://rzp.io/i/test123"
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        BillingCheckoutSession session = provider.createCheckoutSession(tenantId, "pro");

        assertThat(session.sessionId()).isEqualTo("sub_test_123");
        assertThat(session.checkoutUrl()).isEqualTo("https://rzp.io/i/test123");
        assertThat(session.provider()).isEqualTo(BillingProviderType.RAZORPAY);
        server.verify();
    }

    @Test
    void rejectsPlanWithoutConfiguredRazorpayPlanId() {
        RazorpayBillingProvider provider =
                new RazorpayBillingProvider(properties(), RestClient.builder());

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> provider.createCheckoutSession(UUID.randomUUID(), "enterprise"))
                .withMessage("No Razorpay plan is configured for plan: ENTERPRISE");
    }

    @Test
    void convertsRazorpayHttpFailureIntoProviderNeutralException() {
        RazorpayBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayBillingProvider provider = new RazorpayBillingProvider(properties, builder);

        server.expect(requestTo("https://api.razorpay.com/v1/subscriptions"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.createCheckoutSession(UUID.randomUUID(), "PRO"))
                .isInstanceOf(BillingProviderException.class)
                .hasMessage("Razorpay subscription creation failed");
        server.verify();
    }

    @Test
    void cancelsProviderSubscriptionImmediately() {
        RazorpayBillingProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayBillingProvider provider = new RazorpayBillingProvider(properties, builder);

        server.expect(requestTo("https://api.razorpay.com/v1/subscriptions/sub_test_123/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"cancel_at_cycle_end\":false")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        provider.cancelSubscription("sub_test_123");

        server.verify();
    }

    private RazorpayBillingProperties properties() {
        RazorpayBillingProperties properties = new RazorpayBillingProperties();
        properties.setEnabled(true);
        properties.setKeyId("rzp_test_key");
        properties.setKeySecret("test_secret");
        properties.setBaseUrl("https://api.razorpay.com");
        properties.setTotalCount(120);
        properties.setPlans(Map.of("pro", "plan_pro_test"));
        return properties;
    }
}
