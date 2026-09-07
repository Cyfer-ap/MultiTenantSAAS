package com.chacha.multitenantsaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Server-side configuration for encrypting tenant outbound-webhook signing secrets at rest. */
@Component
@ConfigurationProperties(prefix = "app.outbound-webhooks")
public class OutboundWebhookProperties {

    private String encryptionKey;

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
