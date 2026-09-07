package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.config.IdentityFederationProperties;
import com.chacha.multitenantsaas.exception.IdentityFederationUnavailableException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class IdentityProviderSecretCipherTest {

    @Test
    void encryptsAndDecryptsWithoutPersistingPlaintext() {
        IdentityFederationProperties properties = new IdentityFederationProperties();
        properties.setEncryptionKey(base64Key("0123456789abcdef0123456789abcdef"));
        IdentityProviderSecretCipher cipher = new IdentityProviderSecretCipher(properties);

        String plaintext = "oidc-client-secret-123";
        String ciphertext = cipher.encrypt(plaintext);

        assertThat(ciphertext).isNotEqualTo(plaintext);
        assertThat(cipher.decrypt(ciphertext)).isEqualTo(plaintext);
        assertThat(cipher.encrypt(plaintext)).isNotEqualTo(ciphertext);
    }

    @Test
    void failsClosedWhenEncryptionKeyIsMissing() {
        IdentityFederationProperties properties = new IdentityFederationProperties();
        IdentityProviderSecretCipher cipher = new IdentityProviderSecretCipher(properties);

        assertThatThrownBy(() -> cipher.encrypt("oidc-client-secret-123"))
                .isInstanceOf(IdentityFederationUnavailableException.class)
                .hasMessageContaining("IDENTITY_FEDERATION_ENCRYPTION_KEY");
    }

    @Test
    void rejectsMalformedOrWrongLengthKey() {
        IdentityFederationProperties malformed = new IdentityFederationProperties();
        malformed.setEncryptionKey("not-base64***");
        assertThatThrownBy(
                        () ->
                                new IdentityProviderSecretCipher(malformed)
                                        .encrypt("oidc-client-secret-123"))
                .isInstanceOf(IdentityFederationUnavailableException.class)
                .hasMessageContaining("Base64 encoded");

        IdentityFederationProperties tooShort = new IdentityFederationProperties();
        tooShort.setEncryptionKey(base64Key("short-key"));
        assertThatThrownBy(
                        () ->
                                new IdentityProviderSecretCipher(tooShort)
                                        .encrypt("oidc-client-secret-123"))
                .isInstanceOf(IdentityFederationUnavailableException.class)
                .hasMessageContaining("exactly 32 bytes");
    }

    private String base64Key(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
