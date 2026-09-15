package com.chacha.multitenantsaas.projects.access;

import com.chacha.multitenantsaas.entity.ProjectStatus;
import java.util.UUID;

public record ProjectAccessSnapshot(UUID projectId, ProjectStatus status) {

    public void requireMutable() {
        if (status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Archived projects cannot be modified");
        }
    }
}
