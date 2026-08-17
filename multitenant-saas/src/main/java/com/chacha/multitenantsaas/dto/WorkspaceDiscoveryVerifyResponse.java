package com.chacha.multitenantsaas.dto;

import java.util.List;
import java.util.UUID;

public record WorkspaceDiscoveryVerifyResponse(
        List<WorkspaceLoginOptionResponse> workspaces,
        UUID workspaceGrantId,
        String trustedBrowserToken,
        String message) {}
