package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TaskAttachmentInitiateRequest(
        @NotBlank(message = "File name is required") @Size(max = 255, message = "File name cannot exceed 255 characters") String filename,
        @NotBlank(message = "Content type is required") @Size(max = 255, message = "Content type cannot exceed 255 characters") String contentType,
        @Positive(message = "Attachment size must be greater than zero") @Max(value = 26214400, message = "Attachments cannot exceed 25 MiB") long sizeBytes,
        UUID commentId) {}
