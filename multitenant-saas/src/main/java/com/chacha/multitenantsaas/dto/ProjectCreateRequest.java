package com.chacha.multitenantsaas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank(message = "Project name is required") @Size(
                        min = 2,
                        max = 150,
                        message = "Project name must be between 2 and 150 characters")
                String name,
        @Size(max = 2000, message = "Project description cannot exceed 2000 characters") String description) {}
