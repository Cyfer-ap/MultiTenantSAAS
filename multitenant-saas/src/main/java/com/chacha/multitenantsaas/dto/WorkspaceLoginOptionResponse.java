package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record WorkspaceLoginOptionResponse(UUID tenantId, String name, String slug) {}
