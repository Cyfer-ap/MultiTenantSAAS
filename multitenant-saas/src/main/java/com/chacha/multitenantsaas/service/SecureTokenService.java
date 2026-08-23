package com.chacha.multitenantsaas.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class SecureTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateToken() {
        byte[] tokenBytes = new byte[64];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public boolean matchesToken(String rawToken, String expectedHash) {
        if (rawToken == null || expectedHash == null) {
            return false;
        }

        byte[] actual = hashToken(rawToken).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(actual, expected);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte hashedByte : hashedBytes) {
                result.append(String.format("%02x", hashedByte));
            }

            return result.toString();

        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash secure token", exception);
        }
    }
}
