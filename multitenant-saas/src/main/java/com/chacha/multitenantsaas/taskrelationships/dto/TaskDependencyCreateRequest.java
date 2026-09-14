package com.chacha.multitenantsaas.taskrelationships.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TaskDependencyCreateRequest(@NotNull UUID blockingTaskId) {}
