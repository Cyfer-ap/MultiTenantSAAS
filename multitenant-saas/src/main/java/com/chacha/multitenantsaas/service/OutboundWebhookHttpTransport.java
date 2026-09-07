package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.OutboundWebhookDeliveryProperties;
import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryTask;
import com.chacha.multitenantsaas.exception.OutboundWebhookDeliveryException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OutboundWebhookHttpTransport {

    private final OutboundWebhookUrlValidator urlValidator;
    private final OutboundWebhookSecretCipher secretCipher;
    private final OutboundWebhookSigner signer;
    private final OutboundWebhookDeliveryProperties properties;
    private final HttpClient httpClient;

    public OutboundWebhookHttpTransport(
            OutboundWebhookUrlValidator urlValidator,
            OutboundWebhookSecretCipher secretCipher,
            OutboundWebhookSigner signer,
            OutboundWebhookDeliveryProperties properties) {
        this.urlValidator = urlValidator;
        this.secretCipher = secretCipher;
        this.signer = signer;
        this.properties = properties;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(properties.getConnectTimeout())
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
    }

    public int deliver(OutboundWebhookDeliveryTask task) {
        String validatedUrl = urlValidator.validateAndNormalize(task.url());
        String secret = secretCipher.decrypt(task.secretCiphertext());
        long timestamp = Instant.now().getEpochSecond();
        String signature = signer.sign(secret, task.eventId(), timestamp, task.payloadJson());

        HttpRequest request =
                HttpRequest.newBuilder(URI.create(validatedUrl))
                        .timeout(properties.getRequestTimeout())
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "MultiTenantSAAS-Webhooks/1.0")
                        .header("X-MTS-Webhook-Id", task.eventId().toString())
                        .header("X-MTS-Webhook-Delivery-Id", task.deliveryId().toString())
                        .header("X-MTS-Webhook-Event", task.eventType().wireName())
                        .header("X-MTS-Webhook-Timestamp", Long.toString(timestamp))
                        .header("X-MTS-Webhook-Signature", signature)
                        .header(
                                "X-MTS-Webhook-Secret-Version",
                                Integer.toString(task.secretVersion()))
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        task.payloadJson(), StandardCharsets.UTF_8))
                        .build();

        try {
            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new OutboundWebhookDeliveryException(
                        "Webhook endpoint returned HTTP " + status, status);
            }
            return status;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OutboundWebhookDeliveryException(
                    "Webhook delivery was interrupted", null, exception);
        } catch (IOException exception) {
            throw new OutboundWebhookDeliveryException(
                    "Webhook endpoint could not be reached", null, exception);
        }
    }
}
