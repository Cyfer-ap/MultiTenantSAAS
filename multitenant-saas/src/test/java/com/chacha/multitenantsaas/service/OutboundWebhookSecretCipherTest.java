package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.config.OutboundWebhookProperties;
import com.chacha.multitenantsaas.exception.OutboundWebhookUnavailableException;
import org.junit.jupiter.api.Test;

class OutboundWebhookSecretCipherTest {

    @Test
    void encryptsAndDecryptsWithoutPersistingPlaintext() {
        OutboundWebhookProperties properties = new OutboundWebhookProperties();
        properties.setEncryptionKey("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        OutboundWebhookSecretCipher cipher = new OutboundWebhookSecretCipher(properties);

        String encrypted = cipher.encrypt("mwh_example-secret");

        assertThat(encrypted).isNotEqualTo("mwh_example-secret");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("mwh_example-secret");
    }

    @Test
    void failsClosedWhenEncryptionKeyIsMissingOrInvalid() {
        OutboundWebhookProperties missing = new OutboundWebhookProperties();
        OutboundWebhookSecretCipher missingCipher = new OutboundWebhookSecretCipher(missing);

        assertThatThrownBy(() -> missingCipher.encrypt("mwh_secret"))
                .isInstanceOf(OutboundWebhookUnavailableException.class)
                .hasMessageContaining("OUTBOUND_WEBHOOK_ENCRYPTION_KEY");

        OutboundWebhookProperties invalid = new OutboundWebhookProperties();
        invalid.setEncryptionKey("not-base64");
        OutboundWebhookSecretCipher invalidCipher = new OutboundWebhookSecretCipher(invalid);

        assertThatThrownBy(() -> invalidCipher.encrypt("mwh_secret"))
                .isInstanceOf(OutboundWebhookUnavailableException.class)
                .hasMessageContaining("Base64");
    }
}
