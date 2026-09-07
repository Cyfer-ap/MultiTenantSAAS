package com.chacha.multitenantsaas.billing.razorpay;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayPlanRetirementCancellationTest {

    @Test
    void retirementSchedulesCancellationAtCycleEnd() {
        RazorpayBillingProperties properties = new RazorpayBillingProperties();
        properties.setEnabled(true);
        properties.setKeyId("rzp_test_key");
        properties.setKeySecret("rzp_test_secret");
        properties.setBaseUrl("https://api.razorpay.com");
        properties.setTotalCount(12);
        properties.setPlans(Map.of("pro", "plan_pro"));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayBillingProvider provider = new RazorpayBillingProvider(properties, builder);

        server.expect(requestTo("https://api.razorpay.com/v1/subscriptions/sub_test/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"cancel_at_cycle_end\":true")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        provider.scheduleCancellationAtPeriodEnd("sub_test");

        server.verify();
    }
}
