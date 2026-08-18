package com.chacha.multitenantsaas.dto;

import java.time.Instant;

public record TaskAttachmentDownloadResponse(String downloadUrl, Instant expiresAt) {}
