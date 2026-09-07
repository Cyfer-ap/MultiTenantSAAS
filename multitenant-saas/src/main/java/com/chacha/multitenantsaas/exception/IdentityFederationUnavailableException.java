package com.chacha.multitenantsaas.exception;

public class IdentityFederationUnavailableException extends RuntimeException {

    public IdentityFederationUnavailableException(String message) {
        super(message);
    }

    public IdentityFederationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
