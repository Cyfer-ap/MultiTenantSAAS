package com.chacha.multitenantsaas.externalaccess;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.external-access")
public class ExternalAccessProperties {

    private int maxGrantDays = 90;
    private int taskReadLimit = 100;
    private int commentReadLimit = 100;
    private int approvalReadLimit = 50;

    public int getMaxGrantDays() {
        return maxGrantDays;
    }

    public void setMaxGrantDays(int maxGrantDays) {
        this.maxGrantDays = maxGrantDays;
    }

    public int getTaskReadLimit() {
        return taskReadLimit;
    }

    public void setTaskReadLimit(int taskReadLimit) {
        this.taskReadLimit = taskReadLimit;
    }

    public int getCommentReadLimit() {
        return commentReadLimit;
    }

    public void setCommentReadLimit(int commentReadLimit) {
        this.commentReadLimit = commentReadLimit;
    }

    public int getApprovalReadLimit() {
        return approvalReadLimit;
    }

    public void setApprovalReadLimit(int approvalReadLimit) {
        this.approvalReadLimit = approvalReadLimit;
    }
}
