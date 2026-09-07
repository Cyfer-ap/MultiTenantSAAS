package com.chacha.multitenantsaas.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class OidcSecuritySupport {

    private OidcSecuritySupport() {}

    static String randomBase64Url(SecureRandom secureRandom, int byteCount) {
        byte[] bytes = new byte[byteCount];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256(value));
    }

    static String pkceS256Challenge(String verifier) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
    }

    static boolean constantTimeHashMatches(String rawValue, String expectedHexHash) {
        byte[] actual = sha256(rawValue);
        byte[] expected;
        try {
            expected = HexFormat.of().parseHex(expectedHexHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return MessageDigest.isEqual(actual, expected);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
