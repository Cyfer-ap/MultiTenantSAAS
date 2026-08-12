package com.chacha.multitenantsaas.exception;

public class RateLimitExceededException extends RuntimeException {

    private final String scope;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String scope, long retryAfterSeconds) {
        super("Too many requests. Please try again later.");
        this.scope = scope;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getScope() {
        return scope;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
