package com.chacha.multitenantsaas.externalaccess;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.external-access")
public class ExternalAccessProperties {

    private int maxGrantDays = 90;
    private int sessionHours = 8;
    private int taskReadLimit = 100;

    public int getMaxGrantDays() {
        return maxGrantDays;
    }

    public void setMaxGrantDays(int maxGrantDays) {
        this.maxGrantDays = maxGrantDays;
    }

    public int getSessionHours() {
        return sessionHours;
    }

    public void setSessionHours(int sessionHours) {
        this.sessionHours = sessionHours;
    }

    public int getTaskReadLimit() {
        return taskReadLimit;
    }

    public void setTaskReadLimit(int taskReadLimit) {
        this.taskReadLimit = taskReadLimit;
    }
}
