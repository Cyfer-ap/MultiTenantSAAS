package com.chacha.multitenantsaas.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class OutboundWebhookSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String sign(String secret, UUID eventId, long timestampSeconds, String payloadJson) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Webhook signing secret must not be blank");
        }
        if (eventId == null) {
            throw new IllegalArgumentException("Webhook event id must not be null");
        }
        if (payloadJson == null) {
            throw new IllegalArgumentException("Webhook payload must not be null");
        }

        String signedPayload = timestampSeconds + "." + eventId + "." + payloadJson;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return "v1="
                    + HexFormat.of()
                            .formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign outbound webhook request", exception);
        }
    }
}
