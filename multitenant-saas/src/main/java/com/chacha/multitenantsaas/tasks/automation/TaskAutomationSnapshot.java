package com.chacha.multitenantsaas.tasks.automation;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;

public record TaskAutomationSnapshot(ProjectTaskStatus status, ProjectTaskPriority priority) {}
