package com.chacha.multitenantsaas.billing.razorpay;

import com.chacha.multitenantsaas.billing.entity.BillingProviderEnvironment;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.billing.razorpay")
public class RazorpayBillingProperties {

    private boolean enabled;
    private BillingProviderEnvironment environment = BillingProviderEnvironment.TEST;
    private boolean webhookEnabled;
    private String webhookSecret = "";
    private String keyId = "";
    private String keySecret = "";
    private String baseUrl = "https://api.razorpay.com";
    private int totalCount = 120;
    private Map<String, String> plans = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BillingProviderEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(BillingProviderEnvironment environment) {
        this.environment = environment;
    }

    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }

    public void setWebhookEnabled(boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public Map<String, String> getPlans() {
        return plans;
    }

    public void setPlans(Map<String, String> plans) {
        this.plans = plans;
    }
}
