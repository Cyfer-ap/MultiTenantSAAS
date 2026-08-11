package com.chacha.multitenantsaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.public-rate-limit")
public class PublicAuthRateLimitProperties {

    private boolean enabled = true;
    private long windowSeconds = 60L;
    private int loginMaxRequests = 30;
    private int recoveryMaxRequests = 10;
    private int tokenMaxRequests = 120;
    private int onboardingMaxRequests = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getLoginMaxRequests() {
        return loginMaxRequests;
    }

    public void setLoginMaxRequests(int loginMaxRequests) {
        this.loginMaxRequests = loginMaxRequests;
    }

    public int getRecoveryMaxRequests() {
        return recoveryMaxRequests;
    }

    public void setRecoveryMaxRequests(int recoveryMaxRequests) {
        this.recoveryMaxRequests = recoveryMaxRequests;
    }

    public int getTokenMaxRequests() {
        return tokenMaxRequests;
    }

    public void setTokenMaxRequests(int tokenMaxRequests) {
        this.tokenMaxRequests = tokenMaxRequests;
    }

    public int getOnboardingMaxRequests() {
        return onboardingMaxRequests;
    }

    public void setOnboardingMaxRequests(int onboardingMaxRequests) {
        this.onboardingMaxRequests = onboardingMaxRequests;
    }
}
