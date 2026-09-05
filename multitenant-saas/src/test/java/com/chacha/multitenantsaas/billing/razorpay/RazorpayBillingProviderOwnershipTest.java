package com.chacha.multitenantsaas.billing.razorpay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayBillingProviderOwnershipTest {

    @Test
    void verifiesOwnershipFromSubscriptionIdentityWithoutLifecycleParsing() {
        RazorpayBillingProperties properties = new RazorpayBillingProperties();
        properties.setKeyId("rzp_test_key");
        properties.setKeySecret("rzp_test_secret");
        properties.setBaseUrl("https://api.razorpay.test");
        properties.setTotalCount(12);
        properties.setPlans(Map.of("PRO", "plan_pro"));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayBillingProvider provider = new RazorpayBillingProvider(properties, builder);

        server.expect(
                        org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                                "https://api.razorpay.test/v1/subscriptions/sub_rzp_123"))
                .andRespond(
                        org.springframework.test.web.client.response.MockRestResponseCreators
                                .withSuccess(
                                        "{\"id\":\"sub_rzp_123\",\"status\":\"active\"}",
                                        MediaType.APPLICATION_JSON));

        assertThat(provider.ownsSubscription("sub_rzp_123")).isTrue();
        server.verify();
    }
}
