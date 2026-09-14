package com.chacha.multitenantsaas.taskrelationships.dto;

import java.util.List;

public record TaskRelationshipsResponse(
        TaskReferenceResponse parent,
        List<TaskReferenceResponse> children,
        List<TaskReferenceResponse> blockers,
        List<TaskReferenceResponse> dependents,
        List<TaskLabelResponse> labels,
        boolean childrenTruncated) {}
