package com.chacha.multitenantsaas.exception;

public class OutboundWebhookUnavailableException extends RuntimeException {

    public OutboundWebhookUnavailableException(String message) {
        super(message);
    }

    public OutboundWebhookUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
