package com.chacha.multitenantsaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth.email-verification")
public class EmailVerificationProperties {

    private long expirationMinutes = 10L;
    private int maxAttempts = 5;
    private long trustedBrowserDays = 30L;
    private String secret;
    private boolean requireLoginGrant = true;

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getTrustedBrowserDays() {
        return trustedBrowserDays;
    }

    public void setTrustedBrowserDays(long trustedBrowserDays) {
        this.trustedBrowserDays = trustedBrowserDays;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isRequireLoginGrant() {
        return requireLoginGrant;
    }

    public void setRequireLoginGrant(boolean requireLoginGrant) {
        this.requireLoginGrant = requireLoginGrant;
    }
}
