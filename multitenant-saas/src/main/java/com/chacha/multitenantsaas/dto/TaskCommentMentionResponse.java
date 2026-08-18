package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record TaskCommentMentionResponse(UUID userId, String fullName, String email) {}
