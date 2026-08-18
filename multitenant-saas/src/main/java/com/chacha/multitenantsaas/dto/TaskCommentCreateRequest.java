package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record TaskCommentCreateRequest(
        @NotBlank(message = "Comment body is required") @Size(max = 4000, message = "Comment body cannot exceed 4000 characters") String body,
        @Size(max = 20, message = "A comment can mention at most 20 users") Set<UUID> mentionedUserIds) {}
