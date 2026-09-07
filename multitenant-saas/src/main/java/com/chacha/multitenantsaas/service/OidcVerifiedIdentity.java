package com.chacha.multitenantsaas.service;

public record OidcVerifiedIdentity(
        String issuer, String subject, String email, boolean emailVerified) {}
