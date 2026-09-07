package com.chacha.multitenantsaas.exception;

public class IdentityProviderVerificationException extends RuntimeException {

    public IdentityProviderVerificationException(String message) {
        super(message);
    }

    public IdentityProviderVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
