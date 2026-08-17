package com.chacha.multitenantsaas.email;

public record EmailMessage(String to, String subject, String htmlContent) {

    public EmailMessage {
        to = requireText(to, "to");
        subject = requireText(subject, "subject");
        if (htmlContent == null || htmlContent.isBlank()) {
            throw new IllegalArgumentException("htmlContent must not be blank");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
