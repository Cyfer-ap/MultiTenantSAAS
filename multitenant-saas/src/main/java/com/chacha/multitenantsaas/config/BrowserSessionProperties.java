package com.chacha.multitenantsaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth.browser-session")
public class BrowserSessionProperties {

    private boolean cookieMode = true;
    private boolean secure = false;
    private boolean partitioned = false;
    private String sameSite = "Lax";

    public boolean isCookieMode() {
        return cookieMode;
    }

    public void setCookieMode(boolean cookieMode) {
        this.cookieMode = cookieMode;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isPartitioned() {
        return partitioned;
    }

    public void setPartitioned(boolean partitioned) {
        this.partitioned = partitioned;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }
}
