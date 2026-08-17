package com.chacha.multitenantsaas.email;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chacha.multitenantsaas.config.MailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoEmailSenderTest {

    @Test
    void sendsExpectedTransactionalRequestWithoutExposingApiKeyInTheBody() {
        MailProperties properties = brevoProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailSender sender = new BrevoEmailSender(properties, builder);

        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-brevo-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"email\":\"user@example.com\"")))
                .andExpect(content().string(containsString("\"subject\":\"Security code\"")))
                .andExpect(content().string(containsString("\"htmlContent\":\"<p>123456</p>\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                containsString("test-brevo-key"))))
                .andRespond(withSuccess("{\"messageId\":\"test-id\"}", MediaType.APPLICATION_JSON));

        sender.send(new EmailMessage("user@example.com", "Security code", "<p>123456</p>"));

        server.verify();
    }

    @Test
    void convertsBrevoHttpFailureIntoProviderNeutralException() {
        MailProperties properties = brevoProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailSender sender = new BrevoEmailSender(properties, builder);

        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withServerError());

        assertThatThrownBy(
                        () ->
                                sender.send(
                                        new EmailMessage(
                                                "user@example.com",
                                                "Security code",
                                                "<p>123456</p>")))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessage("Brevo email delivery was rejected");

        server.verify();
    }

    private MailProperties brevoProperties() {
        MailProperties properties = new MailProperties();
        properties.setProvider("brevo");
        properties.setFromEmail("verified-sender@example.com");
        properties.setFromName("MultiTenant SaaS");
        properties.getBrevo().setApiKey("test-brevo-key");
        properties.getBrevo().setBaseUrl("https://api.brevo.com/v3");
        return properties;
    }
}
