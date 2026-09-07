package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.IdentityFederationProperties;
import com.chacha.multitenantsaas.exception.IdentityFederationUnavailableException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class IdentityProviderSecretCipher {

    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final IdentityFederationProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public IdentityProviderSecretCipher(IdentityFederationProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        return encryptValue(plaintext, "Identity-provider client secret");
    }

    public String decrypt(String ciphertext) {
        return decryptValue(ciphertext, "identity-provider client secret");
    }

    public String encryptTransactionSecret(String plaintext) {
        return encryptValue(plaintext, "OIDC authorization transaction secret");
    }

    public String decryptTransactionSecret(String ciphertext) {
        return decryptValue(ciphertext, "OIDC authorization transaction secret");
    }

    private String encryptValue(String plaintext, String label) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }

        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer packed = ByteBuffer.allocate(iv.length + encrypted.length);
            packed.put(iv);
            packed.put(encrypted);
            return Base64.getEncoder().encodeToString(packed.array());
        } catch (GeneralSecurityException exception) {
            throw new IdentityFederationUnavailableException(
                    "Could not protect " + label, exception);
        }
    }

    private String decryptValue(String ciphertext, String label) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IdentityFederationUnavailableException("Stored " + label + " is unavailable");
        }

        try {
            byte[] packed = Base64.getDecoder().decode(ciphertext);
            if (packed.length <= IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext is too short");
            }
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new IdentityFederationUnavailableException(
                    "Could not decrypt " + label, exception);
        }
    }

    private SecretKeySpec key() {
        String configured = properties.getEncryptionKey();
        if (configured == null || configured.isBlank()) {
            throw new IdentityFederationUnavailableException(
                    "Identity federation is unavailable until IDENTITY_FEDERATION_ENCRYPTION_KEY is configured");
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured.trim());
        } catch (IllegalArgumentException exception) {
            throw new IdentityFederationUnavailableException(
                    "IDENTITY_FEDERATION_ENCRYPTION_KEY must be Base64 encoded", exception);
        }

        if (decoded.length != KEY_BYTES) {
            throw new IdentityFederationUnavailableException(
                    "IDENTITY_FEDERATION_ENCRYPTION_KEY must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(decoded, "AES");
    }
}
