package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboundWebhookSignerTest {

    private final OutboundWebhookSigner signer = new OutboundWebhookSigner();

    @Test
    void producesStableVersionedHmacSignature() {
        String signature =
                signer.sign(
                        "mwh_test-secret",
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        1_700_000_000L,
                        "{\"id\":\"abc\"}");

        assertThat(signature)
                .isEqualTo("v1=dc135f138b32ac79fddc72a9035e6c6f17d5b220a9d854ff9f633fe6f3433c89");
    }

    @Test
    void signatureChangesWhenReplaySensitiveInputsChange() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String first = signer.sign("mwh_test-secret", eventId, 100L, "{\"value\":1}");
        String changedTimestamp = signer.sign("mwh_test-secret", eventId, 101L, "{\"value\":1}");
        String changedBody = signer.sign("mwh_test-secret", eventId, 100L, "{\"value\":2}");

        assertThat(changedTimestamp).isNotEqualTo(first);
        assertThat(changedBody).isNotEqualTo(first);
    }
}
