package com.chacha.multitenantsaas.billing.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WebhookSignatureSupport {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private WebhookSignatureSupport() {}

    public static boolean matches(String secret, String message, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            byte[] provided = HexFormat.of().parseHex(signature.trim());
            return MessageDigest.isEqual(sign(secret, message), provided);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static String signAsHex(String secret, String message) {
        return HexFormat.of().formatHex(sign(secret, message));
    }

    private static byte[] sign(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
