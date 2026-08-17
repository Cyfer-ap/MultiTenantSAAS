package com.chacha.multitenantsaas.dto;

import java.util.List;
import java.util.UUID;

public record WorkspaceDiscoveryStartResponse(
        boolean verificationRequired,
        UUID challengeId,
        List<WorkspaceLoginOptionResponse> workspaces,
        UUID workspaceGrantId,
        long expiresInSeconds,
        String message) {}
