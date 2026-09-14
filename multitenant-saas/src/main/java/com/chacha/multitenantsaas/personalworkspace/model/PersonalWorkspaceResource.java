package com.chacha.multitenantsaas.personalworkspace.model;

import java.util.UUID;

public record PersonalWorkspaceResource(
        PersonalResourceType type, UUID resourceId, UUID parentId, String title, String subtitle) {}
