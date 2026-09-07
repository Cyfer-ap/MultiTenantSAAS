package com.chacha.multitenantsaas.exception;

public class OutboundWebhookDeliveryException extends RuntimeException {

    private final Integer httpStatus;

    public OutboundWebhookDeliveryException(String message, Integer httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public OutboundWebhookDeliveryException(String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
