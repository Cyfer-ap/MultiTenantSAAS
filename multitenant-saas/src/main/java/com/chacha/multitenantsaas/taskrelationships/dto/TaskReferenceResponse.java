package com.chacha.multitenantsaas.taskrelationships.dto;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import java.util.UUID;

public record TaskReferenceResponse(
        UUID id, String title, ProjectTaskStatus status, ProjectTaskPriority priority) {}
