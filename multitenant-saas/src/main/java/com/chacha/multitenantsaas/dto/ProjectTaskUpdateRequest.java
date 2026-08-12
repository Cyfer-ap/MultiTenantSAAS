package com.chacha.multitenantsaas.dto;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ProjectTaskUpdateRequest(
        @NotBlank(message = "Task title is required") @Size(
                        min = 2,
                        max = 200,
                        message = "Task title must be between 2 and 200 characters")
                String title,
        @Size(max = 4000, message = "Task description cannot exceed 4000 characters") String description,
        @NotNull(message = "Task priority is required") ProjectTaskPriority priority,
        Instant dueAt) {}
