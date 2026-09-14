package com.chacha.multitenantsaas.taskrelationships.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectTaskLabelRequest(
        @NotBlank @Size(max = 60) String name,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a six-digit hex value") String color) {}
