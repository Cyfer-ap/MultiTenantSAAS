package com.chacha.multitenantsaas.dto;

import java.time.Instant;

public record OidcAuthorizationStartResponse(String authorizationUrl, Instant expiresAt) {}
