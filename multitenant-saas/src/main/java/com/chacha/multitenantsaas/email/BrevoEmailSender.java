package com.chacha.multitenantsaas.email;

import com.chacha.multitenantsaas.config.MailProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class BrevoEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);

    private final MailProperties properties;
    private final RestClient restClient;

    public BrevoEmailSender(MailProperties properties) {
        this(properties, RestClient.builder());
    }

    BrevoEmailSender(MailProperties properties, RestClient.Builder builder) {
        validateConfiguration(properties);
        this.properties = properties;
        this.restClient =
                builder.baseUrl(properties.getBrevo().getBaseUrl())
                        .defaultHeader("api-key", properties.getBrevo().getApiKey())
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build();
    }

    @Override
    public void send(EmailMessage message) {
        BrevoSendRequest request =
                new BrevoSendRequest(
                        new Sender(properties.getFromName(), properties.getFromEmail()),
                        List.of(new Recipient(message.to())),
                        message.subject(),
                        message.htmlContent());

        try {
            restClient
                    .post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn(
                    "Brevo rejected email delivery with HTTP status {}",
                    ex.getStatusCode().value());
            throw new EmailDeliveryException("Brevo email delivery was rejected", ex);
        } catch (RestClientException ex) {
            log.warn("Brevo email delivery failed before a successful response was received");
            throw new EmailDeliveryException("Brevo email delivery failed", ex);
        }
    }

    private static void validateConfiguration(MailProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("Mail properties are required");
        }
        requireConfigured(properties.getFromEmail(), "MAIL_FROM_EMAIL");
        requireConfigured(properties.getFromName(), "MAIL_FROM_NAME");
        if (properties.getBrevo() == null) {
            throw new IllegalStateException("Brevo mail properties are required");
        }
        requireConfigured(properties.getBrevo().getApiKey(), "BREVO_API_KEY");
        requireConfigured(properties.getBrevo().getBaseUrl(), "BREVO_BASE_URL");
    }

    private static void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable + " must be configured for Brevo mail");
        }
    }

    private record BrevoSendRequest(
            Sender sender, List<Recipient> to, String subject, String htmlContent) {}

    private record Sender(String name, String email) {}

    private record Recipient(String email) {}
}
