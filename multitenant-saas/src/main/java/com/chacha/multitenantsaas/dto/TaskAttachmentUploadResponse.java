package com.chacha.multitenantsaas.dto;

import java.time.Instant;
import java.util.Map;

public record TaskAttachmentUploadResponse(
        TaskAttachmentResponse attachment,
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders) {}
