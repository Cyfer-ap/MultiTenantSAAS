package com.chacha.multitenantsaas.email;

import java.io.Serial;

public class EmailDeliveryException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
